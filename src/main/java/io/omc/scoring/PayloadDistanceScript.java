/* 
 * Copyright (C) One More Cloud, Inc - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential.
 *
 */
package io.omc.scoring;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.search.lookup.IndexField;
import org.elasticsearch.search.lookup.IndexFieldTerm;
import org.elasticsearch.search.lookup.IndexLookup;
import org.elasticsearch.search.lookup.TermPosition;

/**
 * 
 * Custom script which computes distance between payload values and input values. When the script is executed, a set of (term, double) pairs
 * are provided, which are used to determine distance from the fields (term, double) payload values.
 * 
 * @author Dan Simpson
 *
 */
public class PayloadDistanceScript extends AbstractDoubleSearchScript {

	protected static final Logger log = Loggers.getLogger(PayloadDistanceScript.class);

	private final Stream<DistanceScoreSettings> scorers;

	private class DistanceScoreSettings {

		final String field;
		final Map<String, Double> termValues;
		final double missingFactor;
		final double matchBoost;

		@SuppressWarnings("unchecked")
		public DistanceScoreSettings(Map<String, Object> settings) {
			field = (String) settings.get("field");
			termValues = (Map<String, Double>) settings.get("term_values");
			missingFactor = (double) settings.getOrDefault("term_missing_factor", 0.2f);
			matchBoost = (double) settings.getOrDefault("term_match_boost", 1d);
		}
	}

	@SuppressWarnings("unchecked")
	public PayloadDistanceScript(Map<String, Object> params) {
		scorers = ((List<Map<String, Object>>) params.get("fields")).stream().map(DistanceScoreSettings::new);
	}

	@Override
	public double runAsDouble() {
		double docScore = scoreOr(1f);
		return scorers.mapToDouble(s -> scoreField(docScore, s)).sum();
	}

	private double scoreField(double docScore, DistanceScoreSettings settings) {
		double score = 0;
		final IndexField index = this.indexLookup().get(settings.field);

		for (Entry<String, Double> entry : settings.termValues.entrySet()) {
			double inputValue = entry.getValue();
			IndexFieldTerm indexTermField = index.get(entry.getKey(), IndexLookup.FLAG_PAYLOADS);

			Float payloadValue = null;
			if (indexTermField != null) {
				Iterator<TermPosition> iter = indexTermField.iterator();
				if (iter.hasNext()) {
					payloadValue = iter.next().payloadAsFloat(0f);
				}
			}

			if (payloadValue != null) {
				score -= docScore * (Math.abs(inputValue - payloadValue) * settings.matchBoost);
			} else {
				score -= docScore * settings.missingFactor;
			}
		}

		return score;
	}

	private float scoreOr(float defaultValue) {
		try {
			return score();
		} catch (IOException e) {
			return defaultValue;
		}
	}

}

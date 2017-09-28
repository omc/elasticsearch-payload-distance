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
import java.util.stream.Collectors;

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

	private static final double DEFAULT_BOOST = 1d;

	private final List<DistanceScoreSettings> scorers;

	private class DistanceScoreSettings {

		final String field;
		final Map<String, Double> termValues;
		final double boost;

		@SuppressWarnings("unchecked")
		public DistanceScoreSettings(Map<String, Object> settings) {
			field = (String) settings.get("field");
			termValues = (Map<String, Double>) settings.get("term_values");
			boost = (double) settings.getOrDefault("boost", DEFAULT_BOOST);
		}
	}

	@SuppressWarnings("unchecked")
	public PayloadDistanceScript(Map<String, Object> params) {
		scorers = ((List<Map<String, Object>>) params.get("fields")).stream().map(DistanceScoreSettings::new)
		    .collect(Collectors.toList());
	}

	@Override
	public double runAsDouble() {
		double score = 0;
		for (DistanceScoreSettings scorer : scorers) {
			score += scoreField(scorer);
		}
		return score + scoreOr(1f);
	}

	/**
	 * For a given field, compute the distance between "term_values" and the fields "term_payload_values". When a term isn't part of the
	 * field, use the term_missing_factor to weight the score. When the term is present, compute the difference, and apply the
	 * term_match_boost factor to weight the score.
	 *
	 * @param docScore
	 *          The original doc score
	 * @param settings
	 *          Settings discovered via parameters
	 *
	 * @return A distance-weighted score
	 */
	private double scoreField(DistanceScoreSettings settings) {
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
				score += Math.min(inputValue, payloadValue) / Math.max(inputValue, payloadValue);
			}
		}

		return score * settings.boost;
	}

	private float scoreOr(float defaultValue) {
		try {
			return score();
		} catch (IOException e) {
			return defaultValue;
		}
	}

}

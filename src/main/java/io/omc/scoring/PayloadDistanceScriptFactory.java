/* 
 * Copyright (C) One More Cloud, Inc - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential.
 *
 */
package io.omc.scoring;

import java.util.Map;

import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

/**
 * 
 * Setup a script for a given request. The script depends on scores as a base.
 * 
 * @author Dan Simpson
 *
 */
public class PayloadDistanceScriptFactory implements NativeScriptFactory {

	final static public String SCRIPT_NAME = "payload_distance_score";

	@Override
	public ExecutableScript newScript(Map<String, Object> params) {
		return new PayloadDistanceScript(params);
	}

	@Override
	public boolean needsScores() {
		return true;
	}

	@Override
	public String getName() {
		return SCRIPT_NAME;
	}

}

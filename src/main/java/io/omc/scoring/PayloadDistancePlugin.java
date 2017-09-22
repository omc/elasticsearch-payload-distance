/* 
 * Copyright (C) One More Cloud, Inc - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Proprietary and confidential.
 *
 */
package io.omc.scoring;

import java.util.Arrays;
import java.util.List;

import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.ScriptPlugin;
import org.elasticsearch.script.NativeScriptFactory;

/**
 * Payload Distance Plugin
 * 
 * @author Dan Simpson
 *
 */
public class PayloadDistancePlugin extends Plugin implements ScriptPlugin {

	@Override
	public List<NativeScriptFactory> getNativeScripts() {
		return Arrays.asList(new PayloadDistanceScriptFactory());
	}
}

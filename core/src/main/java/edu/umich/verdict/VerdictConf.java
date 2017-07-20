package edu.umich.verdict;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;

import edu.umich.verdict.util.VerdictLogger;

public class VerdictConf {
	
    private Map<String, String> configs = new TreeMap<String, String>();
    
    private final Map<String, String> configKeySynonyms =
    		new ImmutableMap.Builder<String, String>()
    		.put("byapss", "verdict.bypass")
    		.put("loglevel", "verdict.loglevel")
    		.build();
    
    public VerdictConf() {
        setDefaults();
    }

    public VerdictConf(Properties properties) {
        this();
        setProperties(properties);
    }
    
    public Map<String, String> getConfigs() {
    	return configs;
    }
    
    public void setProperties(Properties properties) {
    	for (String prop : properties.stringPropertyNames()) {
            this.set(prop, properties.getProperty(prop));
    	}
    }

    public VerdictConf(File file) throws FileNotFoundException {
        this();
        updateFromStream(new FileInputStream(file));
    }

    private VerdictConf setDefaults() {
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            updateFromStream(cl.getResourceAsStream("default.conf"));
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
        
        return this;
    }

    private VerdictConf updateFromStream(InputStream stream) throws FileNotFoundException {
        Scanner scanner = new Scanner(stream);
        while (scanner.hasNext()) {
            String line = scanner.nextLine();
            if (!line.isEmpty() && !line.startsWith("#"))
                set(line);
        }
        scanner.close();
        return this;
    }

    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    public boolean getBoolean(String key) {
        String val = get(key);
        if (val == null)
            return false;
        val = val.toLowerCase();
        return val.equals("on") || val.equals("yes") || val.equals("true") || val.equals("1");
    }

    public double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public double getPercent(String key) {
        String val = get(key);
        if (val.endsWith("%"))
            return Double.parseDouble(val.substring(0, val.length() - 1)) / 100;
        return Double.parseDouble(val);
    }

    public String get(String key) {
    	if (configKeySynonyms.containsKey(key)) {
    		return get(configKeySynonyms.get(key));
    	}
    	return configs.get(key.toLowerCase());
    }
    
    public String getOr(String key, Object defaultValue) {
    	if (configs.containsKey(key.toLowerCase())) {
    		return configs.get(key.toLowerCase());
    	} else {
    		return defaultValue.toString();
    	}
    }

    private VerdictConf set(String keyVal) {
        int equalIndex = keyVal.indexOf('=');
        if (equalIndex == -1)
            return this;
        String key = keyVal.substring(0, equalIndex).trim();
        String val = keyVal.substring(equalIndex + 1).trim();
        if (val.startsWith("\"") && val.endsWith("\""))
            val = val.substring(1, val.length() - 1);
        return set(key, val);
    }

    public VerdictConf set(String key, String value) {
    	key = key.toLowerCase();
    	
    	if (configKeySynonyms.containsKey(key)) {
    		return set(configKeySynonyms.get(key), value);
    	}
    	
    	if (key.equals("verdict.loglevel")) {
    		VerdictLogger.setLogLevel(value);
    	}
    	
        configs.put(key, value);
        return this;
    }

    public Properties toProperties() {
        Properties p = new Properties();
        for (String key : configs.keySet()) {
            p.setProperty(key, configs.get(key));
        }
        return p;
    }
    
    public boolean doesContain(String key) {
    	return configs.containsKey(key.toLowerCase());
    }
    
    /*
	 * Helpers
	 */
    
    // data DBMS
    public void setDbmsSchema(String schema) {
		configs.put("schema", schema);
	}

	public String getDbmsSchema() {
		return get("schema");
	}
	
	public void setDbms(String name) {
		set("dbms", name);
	}
	
	public String getDbms() {
		return get("dbms");
	}
	
	public void setHost(String host) {
		set("host", host);
	}
	
	public String getHost() {
		return get("host");
	}
	
	public void setUser(String user) {
		set("user", user);
	}
	
	public String getUser() {
		return get("user");
	}
	
	public void setPassword(String password) {
		set("password", password);
	}
	
	public String getPassword() {
		return get("password");
	}

	public void setPort(String port) {
		set("port", port);
	}
	
	public String getPort() {
		return get("port");
	}
	
	public double errorBoundConfidenceInPercentage() {
		String p = get("verdict.confidence_internal_probability").replace("%", "");
		return Double.valueOf(p);
	}
	
	public int subsamplingPartitionCount() {
		return getInt("verdict.subsampling_partition_count");
	}
	
	public double getRelativeTargetCost() {
		return getDouble("verdict.relative_target_cost");
	}
	
	public boolean cacheSparkSamples() {
		return getBoolean("verdict.cache_spark_samples");
	}
}

package com.greenlock.frau;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LukeSmalley on 1/19/2017.
 */
public class ScriptProperties {

    private Map<String, String> metadata = new HashMap<>();
    private String hash;
    private String name;
    private boolean isService = false;

    public ScriptProperties(String script) {
        String[] lines = script.split("\n");
        for (String line : lines) {
            if (line.startsWith("#$")) {
                metadata.put(line.substring(2).split("=")[0],
                        line.substring(line.split("=")[0].length() + 1));
            }
        }

        hash = sha256(script);

        if (metadata.containsKey("name")) {
            name = metadata.get("name");
        } else {
            name = hash.substring(0, 8);
        }

        if (metadata.containsKey("service")) {
            isService = Boolean.parseBoolean(metadata.get("service"));
        }
    }

    private String sha256(String input) {
        String output = "deadbeef";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(input.getBytes(Charset.forName("UTF-8")));
            byte[] hash = digest.digest();
            BigInteger bigInt = new BigInteger(1, hash);
            output = bigInt.toString(16);
            while (output.length() < 32) {
                output = "0" + output;
            }
        } catch (NoSuchAlgorithmException ex) {}
        return output;
    }


    public boolean hasMetadataValue(String name) {
        return metadata.containsKey(name);
    }

    public String getMetadataValue(String name) {
        return metadata.containsKey(name) ? metadata.get(name) : "";
    }


    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }

    public boolean isService() {
        return isService;
    }
}

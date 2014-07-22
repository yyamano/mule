package org.mule.osgi;

public class Exporter
{
    public String export(String lang) {
        return lang == "us" ? "Hello!" : "Hola!";
    }
}

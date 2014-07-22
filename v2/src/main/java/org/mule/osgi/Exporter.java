package org.mule.osgi;

public class Exporter
{
    public String export(String lang, boolean greet) {
        if (greet)
        {
            return lang == "us" ? "Hello!" : "Hola!";
        }
        else {
            return lang == "us" ? "Bye!" : "Chau!";
        }
    }
}

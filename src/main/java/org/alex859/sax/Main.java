package org.alex859.sax;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main
{

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException
    {
        MyParser.parse("/home/alessandro.ciccimarra/DevEnv/saxparser/src/main/java/resources/test.xml");
    }
}

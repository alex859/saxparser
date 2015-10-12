package org.alex859.sax;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main
{

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException
    {
        MyParser.parse("/home/alex859/DevEnv/workspace/sax/src/main/resources/test.xml");
    }
}

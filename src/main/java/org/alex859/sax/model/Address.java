package org.alex859.sax.model;

/**
 * @author Alessandro Ciccimarra <alessandro.ciccimarra@gmail.com>
 */
public class Address
{
    private String firstLine;
    private String secondLine;

    public String getFirstLine()
    {
        return firstLine;
    }

    public void setFirstLine(final String firstLine)
    {
        this.firstLine = firstLine;
    }

    public String getSecondLine()
    {
        return secondLine;
    }

    public void setSecondLine(final String secondLine)
    {
        this.secondLine = secondLine;
    }

    @Override
    public String toString()
    {
        return "Address{" +
                "firstLine='" + firstLine + '\'' +
                ", secondLine='" + secondLine + '\'' +
                '}';
    }
}
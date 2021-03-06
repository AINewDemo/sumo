//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.05 at 04:36:39 PM CET 
//


package jrc.it.annotation.reader.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * List of azimuth FM rate values updated along azimuth.
 * 
 * <p>Java class for azimuthFmRateListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="azimuthFmRateListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="azimuthFmRate" type="{}azimuthFmRateType" maxOccurs="1100"/>
 *       &lt;/sequence>
 *       &lt;attribute name="count" use="required" type="{}unsignedInt" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "azimuthFmRateListType", propOrder = {
    "azimuthFmRate"
})
public class AzimuthFmRateListType {

    @XmlElement(required = true)
    protected List<AzimuthFmRateType> azimuthFmRate;
    @XmlAttribute(name = "count", required = true)
    protected long count;

    /**
     * Gets the value of the azimuthFmRate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the azimuthFmRate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAzimuthFmRate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AzimuthFmRateType }
     * 
     * 
     */
    public List<AzimuthFmRateType> getAzimuthFmRate() {
        if (azimuthFmRate == null) {
            azimuthFmRate = new ArrayList<AzimuthFmRateType>();
        }
        return this.azimuthFmRate;
    }

    /**
     * Gets the value of the count property.
     * 
     */
    public long getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     */
    public void setCount(long value) {
        this.count = value;
    }

}

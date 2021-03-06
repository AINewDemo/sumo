//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.03.05 at 04:36:39 PM CET 
//


package jrc.it.annotation.reader.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Annotation record for SRGR/GRSR conversion information.
 * 
 * <p>Java class for coordinateConversionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="coordinateConversionType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="azimuthTime" type="{}timeType"/>
 *         &lt;element name="slantRangeTime" type="{}double"/>
 *         &lt;element name="sr0" type="{}double"/>
 *         &lt;element name="srgrCoefficients" type="{}doubleCoefficientArray"/>
 *         &lt;element name="gr0" type="{}double"/>
 *         &lt;element name="grsrCoefficients" type="{}doubleCoefficientArray"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "coordinateConversionType", propOrder = {
    "azimuthTime",
    "slantRangeTime",
    "sr0",
    "srgrCoefficients",
    "gr0",
    "grsrCoefficients"
})
public class CoordinateConversionType {

    @XmlElement(required = true)
    protected XMLGregorianCalendar azimuthTime;
    @XmlElement(required = true)
    protected Double slantRangeTime;
    @XmlElement(required = true)
    protected Double sr0;
    @XmlElement(required = true)
    protected DoubleCoefficientArray srgrCoefficients;
    @XmlElement(required = true)
    protected Double gr0;
    @XmlElement(required = true)
    protected DoubleCoefficientArray grsrCoefficients;

    /**
     * Gets the value of the azimuthTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getAzimuthTime() {
        return azimuthTime;
    }

    /**
     * Sets the value of the azimuthTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setAzimuthTime(XMLGregorianCalendar value) {
        this.azimuthTime = value;
    }

    /**
     * Gets the value of the slantRangeTime property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getSlantRangeTime() {
        return slantRangeTime;
    }

    /**
     * Sets the value of the slantRangeTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSlantRangeTime(Double value) {
        this.slantRangeTime = value;
    }

    /**
     * Gets the value of the sr0 property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getSr0() {
        return sr0;
    }

    /**
     * Sets the value of the sr0 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setSr0(Double value) {
        this.sr0 = value;
    }

    /**
     * Gets the value of the srgrCoefficients property.
     * 
     * @return
     *     possible object is
     *     {@link DoubleCoefficientArray }
     *     
     */
    public DoubleCoefficientArray getSrgrCoefficients() {
        return srgrCoefficients;
    }

    /**
     * Sets the value of the srgrCoefficients property.
     * 
     * @param value
     *     allowed object is
     *     {@link DoubleCoefficientArray }
     *     
     */
    public void setSrgrCoefficients(DoubleCoefficientArray value) {
        this.srgrCoefficients = value;
    }

    /**
     * Gets the value of the gr0 property.
     * 
     * @return
     *     possible object is
     *     {@link Double }
     *     
     */
    public Double getGr0() {
        return gr0;
    }

    /**
     * Sets the value of the gr0 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Double }
     *     
     */
    public void setGr0(Double value) {
        this.gr0 = value;
    }

    /**
     * Gets the value of the grsrCoefficients property.
     * 
     * @return
     *     possible object is
     *     {@link DoubleCoefficientArray }
     *     
     */
    public DoubleCoefficientArray getGrsrCoefficients() {
        return grsrCoefficients;
    }

    /**
     * Sets the value of the grsrCoefficients property.
     * 
     * @param value
     *     allowed object is
     *     {@link DoubleCoefficientArray }
     *     
     */
    public void setGrsrCoefficients(DoubleCoefficientArray value) {
        this.grsrCoefficients = value;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.06.24 at 04:43:09 PM CEST 
//


package safe.annotation;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}dcMethod"/>
 *         &lt;element ref="{}dopplerCentroidUncertainFlag"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "dcMethod",
    "dopplerCentroidUncertainFlag"
})
@XmlRootElement(name = "dopplerCentroidQuality")
public class DopplerCentroidQuality {

    @XmlElement(required = true)
    protected String dcMethod;
    protected boolean dopplerCentroidUncertainFlag;

    /**
     * Gets the value of the dcMethod property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDcMethod() {
        return dcMethod;
    }

    /**
     * Sets the value of the dcMethod property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDcMethod(String value) {
        this.dcMethod = value;
    }

    /**
     * Gets the value of the dopplerCentroidUncertainFlag property.
     * 
     */
    public boolean isDopplerCentroidUncertainFlag() {
        return dopplerCentroidUncertainFlag;
    }

    /**
     * Sets the value of the dopplerCentroidUncertainFlag property.
     * 
     */
    public void setDopplerCentroidUncertainFlag(boolean value) {
        this.dopplerCentroidUncertainFlag = value;
    }

}

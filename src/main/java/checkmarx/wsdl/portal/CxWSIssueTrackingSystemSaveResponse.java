//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.11.17 at 10:51:56 PM EST
//


package checkmarx.wsdl.portal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CxWSIssueTrackingSystemSaveResponse complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CxWSIssueTrackingSystemSaveResponse">
 *   &lt;complexContent>
 *     &lt;extension base="{http://Checkmarx.com}CxWSIssueTrackingSystemResponse">
 *       &lt;sequence>
 *         &lt;element name="TrackingSystem" type="{http://Checkmarx.com}CxWSIssueTrackingSystem" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CxWSIssueTrackingSystemSaveResponse", propOrder = {
    "trackingSystem"
})
public class CxWSIssueTrackingSystemSaveResponse
    extends CxWSIssueTrackingSystemResponse
{

    @XmlElement(name = "TrackingSystem")
    protected CxWSIssueTrackingSystem trackingSystem;

    /**
     * Gets the value of the trackingSystem property.
     *
     * @return
     *     possible object is
     *     {@link CxWSIssueTrackingSystem }
     *
     */
    public CxWSIssueTrackingSystem getTrackingSystem() {
        return trackingSystem;
    }

    /**
     * Sets the value of the trackingSystem property.
     *
     * @param value
     *     allowed object is
     *     {@link CxWSIssueTrackingSystem }
     *
     */
    public void setTrackingSystem(CxWSIssueTrackingSystem value) {
        this.trackingSystem = value;
    }

}

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-646
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.11.17 at 10:51:56 PM EST
//


package checkmarx.wsdl.portal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CxWSResponseLDAPServersConfiguration complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CxWSResponseLDAPServersConfiguration">
 *   &lt;complexContent>
 *     &lt;extension base="{http://Checkmarx.com}CxWSBasicRepsonse">
 *       &lt;sequence>
 *         &lt;element name="serverConfigs" type="{http://Checkmarx.com}ArrayOfCxWSLdapServerConfiguration" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CxWSResponseLDAPServersConfiguration", propOrder = {
    "serverConfigs"
})
public class CxWSResponseLDAPServersConfiguration
    extends CxWSBasicRepsonse
{

    protected ArrayOfCxWSLdapServerConfiguration serverConfigs;

    /**
     * Gets the value of the serverConfigs property.
     *
     * @return
     *     possible object is
     *     {@link ArrayOfCxWSLdapServerConfiguration }
     *
     */
    public ArrayOfCxWSLdapServerConfiguration getServerConfigs() {
        return serverConfigs;
    }

    /**
     * Sets the value of the serverConfigs property.
     *
     * @param value
     *     allowed object is
     *     {@link ArrayOfCxWSLdapServerConfiguration }
     *
     */
    public void setServerConfigs(ArrayOfCxWSLdapServerConfiguration value) {
        this.serverConfigs = value;
    }

}

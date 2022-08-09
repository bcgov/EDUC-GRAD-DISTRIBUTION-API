package ca.bc.gov.educ.api.distribution.model.dto;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class Psi {

	private String psiCode;
    private String psiName;    
    private String address1;    
	private String address2;	
	private String address3;
	private String city;	
	private String provinceCode;	
	private String countryCode;    
	private String provinceName;
    private String countryName;
    private String postal;    
    private String cslCode;    
    private String attentionName; 
    private String openFlag;   
    private String fax;    
    private String phone1;    
    private String transmissionMode;  
    private String psisCode;    
    private String psiUrl;
    private String psiGrouping;    
    
	public String getPsiName() {
		return psiName != null ? psiName.trim(): null;
	}

	public String getAddress1() {
		return address1 != null ? address1.trim(): null;
	}
	
	public String getAddress2() {
		return address2 != null ? address2.trim(): null;
	}
	
	public String getAddress3() {
		return address3 != null ? address3.trim(): null;
	}

	public String getCity() {
		return city != null ? city.trim(): null;
	}

	public String getPostal() {
		return postal != null ? postal.trim(): null;
	}
	
	public String getAttentionName() {
		return attentionName != null ? attentionName.trim(): null;
	}
	
	public String getFax() {
		return fax != null ? fax.trim(): null;
	}

	public String getPhone1() {
		return phone1 != null ? phone1.trim(): null;
	}

	public String getPsisCode() {
		return psisCode != null ? psisCode.trim(): null;
	}

	public String getPsiUrl() {
		return psiUrl != null ? psiUrl.trim(): null;
	}

	public String getPsiGrouping() {
		return psiGrouping != null ? psiGrouping.trim(): null;
	}

	public String getProvinceName() {
		return provinceName != null ? provinceName.trim(): null;
	}
	
	public String getCountryName() {
		return countryName != null ? countryName.trim(): null;
	}

	@Override
	public String toString() {
		return "Psi [psiCode=" + psiCode + ", psiName=" + psiName + ", address1=" + address1 + ", address2=" + address2
				+ ", address3=" + address3 + ", city=" + city + ", provinceCode=" + provinceCode + ", countryCode="
				+ countryCode + ", postal=" + postal + ", cslCode=" + cslCode + ", attentionName=" + attentionName
				+ ", openFlag=" + openFlag + ", fax=" + fax + ", phone1=" + phone1 + ", transmissionMode="
				+ transmissionMode + ", psisCode=" + psisCode + ", psiUrl=" + psiUrl + ", psiGrouping=" + psiGrouping
				+ "]";
	} 
    
    
    
}

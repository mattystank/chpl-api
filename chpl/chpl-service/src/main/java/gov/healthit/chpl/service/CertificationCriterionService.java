package gov.healthit.chpl.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.dao.CertificationCriterionDAO;
import gov.healthit.chpl.domain.CertificationCriterion;

@Component
public class CertificationCriterionService {

    private CertificationCriterionDAO certificationCriterionDAO;
    private Environment environment;

    private Map<Long, CertificationCriterion> criteriaMap = new HashMap<Long, CertificationCriterion>();

    @Autowired
    public CertificationCriterionService(CertificationCriterionDAO certificationCriterionDAO, Environment environment) {
        this.certificationCriterionDAO = certificationCriterionDAO;
        this.environment = environment;
    }

    @PostConstruct
    public void postConstruct() {
        criteriaMap = certificationCriterionDAO.findAll().stream()
                .map(criterion -> new CertificationCriterion(criterion))
                .collect(Collectors.toMap(CertificationCriterion::getId, cc -> cc));
    }

    public CertificationCriterion get(Long certificationCriterionId) {
        return criteriaMap.containsKey(certificationCriterionId) ? criteriaMap.get(certificationCriterionId) : null;
    }

    public CertificationCriterion get(String certificationCriterionDescriptor) {
        try {
            return get(Long.parseLong(environment.getProperty(certificationCriterionDescriptor)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class Criteria2015 {
        public static final String A_1 = "criterion.170_315_a_1";
        public static final String A_2 = "criterion.170_315_a_2";
        public static final String A_3 = "criterion.170_315_a_3";
        public static final String A_4 = "criterion.170_315_a_4";
        public static final String A_5 = "criterion.170_315_a_5";
        public static final String A_6 = "criterion.170_315_a_6";
        public static final String A_7 = "criterion.170_315_a_7";
        public static final String A_8 = "criterion.170_315_a_8";
        public static final String A_9 = "criterion.170_315_a_9";
        public static final String A_10 = "criterion.170_315_a_1";
        public static final String A_11 = "criterion.170_315_a_11";
        public static final String A_12 = "criterion.170_315_a_12";
        public static final String A_13 = "criterion.170_315_a_13";
        public static final String A_14 = "criterion.170_315_a_14";
        public static final String A_15 = "criterion.170_315_a_15";
        public static final String B_1 = "criterion.170_315_b_1";
        public static final String B_1_REVISED = "criterion.170_315_b_1_revised";
        public static final String B_2 = "criterion.170_315_b_2";
        public static final String B_2_REVISED = "criterion.170_315_b_2_revised";
        public static final String B_3 = "criterion.170_315_b_3";
        public static final String B_3_REVISED = "criterion.170_315_b_3_revised";
        public static final String B_4 = "criterion.170_315_b_4";
        public static final String B_5 = "criterion.170_315_b_5";
        public static final String B_6 = "criterion.170_315_b_6";
        public static final String B_7 = "criterion.170_315_b_7";
        public static final String B_7_REVISED = "criterion.170_315_b_7_revised";
        public static final String B_8 = "criterion.170_315_b_8";
        public static final String B_8_REVISED = "criterion.170_315_b_8_revised";
        public static final String B_9 = "criterion.170_315_b_9";
        public static final String B_9_REVISED = "criterion.170_315_b_9_revised";
        public static final String B_10 = "criterion.170_315_b_10";
        public static final String C_1 = "criterion.170_315_c_1";
        public static final String C_2 = "criterion.170_315_c_2";
        public static final String C_3 = "criterion.170_315_c_3";
        public static final String C_3_REVISED = "criterion.170_315_c_3_revised";
        public static final String C_4 = "criterion.170_315_c_4";
        public static final String D_1 = "criterion.170_315_d_1";
        public static final String D_2 = "criterion.170_315_d_2";
        public static final String D_2_REVISED = "criterion.170_315_d_2_revised";
        public static final String D_3 = "criterion.170_315_d_3";
        public static final String D_3_REVISED = "criterion.170_315_d_3_revised";
        public static final String D_4 = "criterion.170_315_d_4";
        public static final String D_5 = "criterion.170_315_d_5";
        public static final String D_6 = "criterion.170_315_d_6";
        public static final String D_7 = "criterion.170_315_d_7";
        public static final String D_8 = "criterion.170_315_d_8";
        public static final String D_9 = "criterion.170_315_d_9";
        public static final String D_10 = "criterion.170_315_d_10";
        public static final String D_10_REVISED = "criterion.170_315_d_10_revised";
        public static final String D_11 = "criterion.170_315_d_11";
        public static final String D_12 = "criterion.170_315_d_12";
        public static final String D_13 = "criterion.170_315_d_13";
        public static final String E_1 = "criterion.170_315_e_1";
        public static final String E_1_REVISED = "criterion.170_315_e_1_revised";
        public static final String E_2 = "criterion.170_315_e_2";
        public static final String E_3 = "criterion.170_315_e_3";
        public static final String F_1 = "criterion.170_315_f_1";
        public static final String F_2 = "criterion.170_315_f_2";
        public static final String F_3 = "criterion.170_315_f_3";
        public static final String F_4 = "criterion.170_315_f_4";
        public static final String F_5 = "criterion.170_315_f_5";
        public static final String F_5_REVISED = "criterion.170_315_f_5_revised";
        public static final String F_6 = "criterion.170_315_f_6";
        public static final String F_7 = "criterion.170_315_f_7";
        public static final String G_1 = "criterion.170_315_g_1";
        public static final String G_2 = "criterion.170_315_g_2";
        public static final String G_3 = "criterion.170_315_g_3";
        public static final String G_4 = "criterion.170_315_g_4";
        public static final String G_5 = "criterion.170_315_g_5";
        public static final String G_6 = "criterion.170_315_g_6";
        public static final String G_6_REVISED = "criterion.170_315_g_6_revised";
        public static final String G_7 = "criterion.170_315_g_7";
        public static final String G_8 = "criterion.170_315_g_8";
        public static final String G_9 = "criterion.170_315_g_9";
        public static final String G_9_REVISED = "criterion.170_315_g_9_revised";
        public static final String G_10 = "criterion.170_315_g_10";
        public static final String H_1 = "criterion.170_315_h_1";
        public static final String H_2 = "criterion.170_315_h_2";
    }
}

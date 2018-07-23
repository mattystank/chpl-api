package gov.healthit.chpl.app.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import gov.healthit.chpl.app.AppConfig;
import gov.healthit.chpl.app.LocalContext;
import gov.healthit.chpl.app.LocalContextFactory;
import gov.healthit.chpl.dao.NotificationDAO;
import gov.healthit.chpl.domain.DateRange;
import gov.healthit.chpl.domain.statistics.CertifiedBodyAltTestStatistics;
import gov.healthit.chpl.domain.statistics.CertifiedBodyStatistics;
import gov.healthit.chpl.domain.statistics.Statistics;

/**
 * Generates summary statistics.
 * @author alarned
 *
 */
@Component("summaryStatistics")
public class SummaryStatistics {
    private static final String DEFAULT_PROPERTIES_FILE = "environment.properties";
    private static final Logger LOGGER = LogManager.getLogger(SummaryStatistics.class);
    private static final int START_DATE_ARG_LOCATION = 0;
    private static final int END_DATE_ARG_LOCATION = 1;
    private static final int NUM_DAYS_ARG_LOCATION = 2;
    private static final int GEN_CSV_ARG_LOCATION = 3;
    private static Date startDate;
    private static Date endDate;
    private static Integer numDaysInPeriod;
    private static boolean generateCsv;
    private Properties props;
    private AsynchronousStatisticsInitializor asynchronousStatisticsInitializor;
    private NotificationDAO notificationDao;

    /**
     * Default constructor.
     */
    public SummaryStatistics() {
    }

    /**
     * This application generates a weekly summary email with an attached CSV.
     * providing CHPL statistics
     * @param args startDate, endDate, numDaysInPeriod, whether or not to generate the CSV
     * @throws Exception some exception
     */
    public static void main(final String[] args) throws Exception {
        SummaryStatistics summaryStats = new SummaryStatistics();
        summaryStats.parseCommandLineArgs(args); // sets startDate, endDate, numDaysInPeriod
        InputStream in = SummaryStatistics.class.getClassLoader().getResourceAsStream(DEFAULT_PROPERTIES_FILE);
        Properties props = summaryStats.loadProperties(in);
        LocalContext ctx = LocalContextFactory.createLocalContext(summaryStats.props.getProperty("dbDriverClass"));
        ctx.addDataSource(summaryStats.props.getProperty("dataSourceName"),
                summaryStats.props.getProperty("dataSourceConnection"),
                summaryStats.props.getProperty("dataSourceUsername"),
                summaryStats.props.getProperty("dataSourcePassword"));
        AbstractApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        summaryStats.initializeSpringClasses(context);
        Future<Statistics> futureEmailBodyStats = summaryStats.asynchronousStatisticsInitializor
                .getStatistics(null);
        Statistics emailBodyStats = futureEmailBodyStats.get();
        List<File> files = new ArrayList<File>();

        if (generateCsv) {
            List<Statistics> csvStats = new ArrayList<Statistics>();
            Calendar startDateCal = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
            startDateCal.setTime(startDate);
            Calendar endDateCal = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
            endDateCal.setTime(startDate);
            endDateCal.add(Calendar.DATE, numDaysInPeriod);

            while (endDate.compareTo(endDateCal.getTime()) >= 0) {
                LOGGER.info("Getting csvRecord for start date " + startDateCal.getTime().toString() + " end date "
                        + endDateCal.getTime().toString());
                System.out.println("Getting csvRecord for start date "
                        + startDateCal.getTime().toString() + " end date "
                        + endDateCal.getTime().toString());
                DateRange csvRange = new DateRange(startDateCal.getTime(), new Date(endDateCal.getTimeInMillis()));
                Statistics historyStat = new Statistics();
                historyStat.setDateRange(csvRange);
                Future<Statistics> futureEmailCsvStats = summaryStats.asynchronousStatisticsInitializor
                        .getStatistics(csvRange);
                historyStat = futureEmailCsvStats.get();
                csvStats.add(historyStat);
                LOGGER.info("Finished getting csvRecord for start date "
                        + startDateCal.getTime().toString() + " end date "
                        + endDateCal.getTime().toString());
                System.out.println("Finished getting csvRecord for start date "
                        + startDateCal.getTime().toString() + " end date "
                        + endDateCal.getTime().toString());
                startDateCal.add(Calendar.DATE, numDaysInPeriod);
                endDateCal.setTime(startDateCal.getTime());
                endDateCal.add(Calendar.DATE, numDaysInPeriod);
            }
            LOGGER.info("Finished getting statistics");
            System.out.println("Finished getting statistics");
            StatsCsvFileWriter.writeCsvFile(props.getProperty("downloadFolderPath") + File.separator
                    + props.getProperty("summaryEmailName", "summaryStatistics.csv"), csvStats);

            File csvFile = new File(props.getProperty("downloadFolderPath") + File.separator
                    + props.getProperty("summaryEmailName", "summaryStatistics.csv"));
            files.add(csvFile);
        }
        String htmlMessage = summaryStats.createHtmlMessage(emailBodyStats);
        LOGGER.info(htmlMessage);

        // send the email
        /*
        Set<GrantedPermission> permissions = new HashSet<GrantedPermission>();
        permissions.add(new GrantedPermission("ROLE_ADMIN"));
        List<RecipientWithSubscriptionsDTO> recipients = summaryStats.getNotificationDao()
                .getAllNotificationMappingsForType(permissions, NotificationTypeConcept.SUMMARY_STATISTICS, null);
        if (recipients != null && recipients.size() > 0) {
            String[] emailAddrs = new String[recipients.size()];
            for (int i = 0; i < recipients.size(); i++) {
                RecipientWithSubscriptionsDTO recip = recipients.get(i);
                emailAddrs[i] = recip.getEmail();
                LOGGER.info("Sending email to " + recip.getEmail());
            }
            SendMailUtil mailUtil = new SendMailUtil();
            mailUtil.sendEmail(null, emailAddrs, props.getProperty("summaryEmailSubject").toString(), htmlMessage,
                    files, props);
        }
        */
        LOGGER.info("Completed SummaryStatistics execution.");
        context.close();
    }

    /**
     * Updates the startDate, endDate, and numDaysInPeriod using the command-line arguments.
     *
     * @param args startDate, endDate, numDaysInPeriod
     * @throws Exception some exception
     */
    private void parseCommandLineArgs(final String[] args) throws Exception {
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
        isoFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
        final int withoutNumDays = 2;
        final int withNumDays = 3;
        final int withAllParams = 4;
        final int defaultDays = 7;
        Integer numArgs = args.length;
        switch (numArgs) {
        case withoutNumDays:
            try {
                startDate = isoFormat.parse(args[START_DATE_ARG_LOCATION]);
                endDate = isoFormat.parse(args[END_DATE_ARG_LOCATION]);
                numDaysInPeriod = defaultDays;
                generateCsv = true;
            } catch (final ParseException e) {
                throw new ParseException(
                        "Please enter startDate and endDate command-line arguments in the format of yyyy-MM-dd",
                        e.getErrorOffset());
            }
            break;
        case withNumDays:
            try {
                startDate = isoFormat.parse(args[START_DATE_ARG_LOCATION]);
                endDate = isoFormat.parse(args[END_DATE_ARG_LOCATION]);
                numDaysInPeriod = Integer.parseInt(args[NUM_DAYS_ARG_LOCATION]);
                generateCsv = true;
            } catch (final ParseException e) {
                throw new ParseException(
                        "Please enter startDate and endDate command-line arguments in the format of yyyy-MM-dd",
                        e.getErrorOffset());
            } catch (final NumberFormatException e) {
                LOGGER.info("Third command line argument could not be parsed to integer. " + e.getMessage());
                numDaysInPeriod = defaultDays;
            }
            break;
        case withAllParams:
            try {
                startDate = isoFormat.parse(args[START_DATE_ARG_LOCATION]);
                endDate = isoFormat.parse(args[END_DATE_ARG_LOCATION]);
                numDaysInPeriod = Integer.parseInt(args[NUM_DAYS_ARG_LOCATION]);
                generateCsv = Boolean.parseBoolean(args[GEN_CSV_ARG_LOCATION]);
            } catch (final ParseException e) {
                throw new ParseException(
                        "Please enter startDate and endDate command-line arguments in the format of yyyy-MM-dd",
                        e.getErrorOffset());
            } catch (final NumberFormatException e) {
                LOGGER.info("Third command line argument could not be parsed to integer. " + e.getMessage());
                numDaysInPeriod = defaultDays;
            }
            break;
        default:
            throw new Exception(
                    "ParseActivities expects two, three or four command-line arguments: "
                            + "startDate, endDate, optionally numDaysInPeriod, "
                            + "optionally whether or not to generate a CSV");
        }
    }

    /**
     * Get relevant beans.
     * @param context the application context
     */
    private void initializeSpringClasses(final AbstractApplicationContext context) {
        LOGGER.info(context.getClassLoader());
        setAsynchronousStatisticsInitializor(
                (AsynchronousStatisticsInitializor) context.getBean("asynchronousStatisticsInitializor"));
        setNotificationDao((NotificationDAO) context.getBean("notificationDAO"));

    }

    /**
     * Set the ParseActivities.Properties (props) using an InputStream to get
     * all properties from the InputStream
     *
     * @param in incoming input stream
     * @return the properties file
     * @throws IOException if unable to read properties
     */
    private Properties loadProperties(final InputStream in) throws IOException {
        if (in == null) {
            props = null;
            throw new FileNotFoundException("Environment Properties File not found in class path.");
        } else {
            props = new Properties();
            props.load(in);
            in.close();
        }
        return props;
    }

    private String createHtmlMessage(final Statistics stats) {
        StringBuilder emailMessage = new StringBuilder();

        emailMessage.append(createMessageHeader());
        emailMessage.append(createUniqueDeveloperSection(stats));
        emailMessage.append(createUniqueProductSection(stats));
        emailMessage.append(createListingSection(stats));

        emailMessage.append(
                "<h4>Total # of Surveillance Activities -  " + stats.getTotalSurveillanceActivities() + "</h4>");
        emailMessage.append(
                "<ul><li>Open Surveillance Activities - " + stats.getTotalOpenSurveillanceActivities() + "</li>");
        emailMessage.append(
                "<li>Closed Surveillance Activities - " + stats.getTotalClosedSurveillanceActivities() + "</li></ul>");
        emailMessage.append("<h4>Total # of NCs -  " + stats.getTotalNonConformities() + "</h4>");
        emailMessage.append("<ul><li>Open NCs - " + stats.getTotalOpenNonconformities() + "</li>");
        emailMessage.append("<li>Closed NCs - " + stats.getTotalClosedNonconformities() + "</li></ul>");
        LOGGER.info(emailMessage.toString());
        return emailMessage.toString();
    }

    private String createMessageHeader() {
        Calendar currDateCal = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
        Calendar endDateCal = Calendar.getInstance(TimeZone.getTimeZone(ZoneOffset.UTC));
        endDateCal.setTime(endDate);
        StringBuilder ret = new StringBuilder();
        ret.append("Email body has current statistics as of " + currDateCal.getTime());
        ret.append("<br/>");
        ret.append("Email attachment has weekly statistics ending " + endDateCal.getTime());
        return ret.toString();
    }

    private String createUniqueDeveloperSection(final Statistics stats) {
        final int edition2014 = 2014;
        final int edition2015 = 2015;
        List<String> uniqueAcbList = new ArrayList<String>();
        Boolean hasSuspended = false;
        StringBuilder ret = new StringBuilder();

        ret.append(
                "<h4>Total # of Unique Developers (Regardless of Edition) -  " + stats.getTotalDevelopers() + "</h4>");
        ret.append("<ul><li>Total # of Developers with Active 2014 Listings - "
                + stats.getTotalDevelopersWithActive2014Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalDevelopersByCertifiedBodyWithListingsEachYear()) {
            if (cbStat.getYear() == edition2014 && getActiveDevelopersForAcb(edition2014,
                    stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                    cbStat.getName()) > 0) {

                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getActiveDevelopersForAcb(edition2014,
                                stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                                cbStat.getName())
                        + "</li>");
            }
        }
        ret.append("</ul>");
        ret.append("<li>Total # of Developers with Suspended by ONC-ACB/Suspended by ONC 2014 Listings</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats
                .getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear()) {
            if (cbStat.getYear() == edition2014
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                if (!uniqueAcbList.contains(cbStat.getName())) {
                    ret.append("<li>Certified by " + cbStat.getName() + " - "
                            + getSuspendedDevelopersForAcb(edition2014,
                                    stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                                    cbStat.getName())
                            + "</li>");
                    uniqueAcbList.add(cbStat.getName());
                    hasSuspended = true;
                }
            }
        }
        ret.append("</ul>");
        if (!hasSuspended) {
            ret.append("<ul><li>No certified bodies have suspended listings</li></ul>");
        }

        ret.append("<li>Total # of Developers with 2014 Listings (Regardless of Status) - "
                + stats.getTotalDevelopersWith2014Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalDevelopersByCertifiedBodyWithListingsEachYear()) {
            if (cbStat.getYear() == edition2014 && cbStat.getTotalDevelopersWithListings() > 0) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + cbStat.getTotalDevelopersWithListings() + "</li>");
            }
        }
        ret.append("</ul>");

        ret.append("<li>Total # of Developers with Active 2015 Listings - "
                + stats.getTotalDevelopersWithActive2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalDevelopersByCertifiedBodyWithListingsEachYear()) {
            if (cbStat.getYear() == edition2015 && getActiveDevelopersForAcb(edition2015,
                    stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                    cbStat.getName()) > 0) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getActiveDevelopersForAcb(edition2015,
                                stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                                cbStat.getName())
                        + "</li>");
            }
        }
        ret.append("</ul>");

        ret.append("<li>Total # of Developers with Suspended by ONC-ACB/Suspended by ONC 2015 Listings</li>");
        uniqueAcbList.clear(); // make sure not to add one ACB more than once
        hasSuspended = false;
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats
                .getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear()) {
            if (cbStat.getYear() == edition2015
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                if (!uniqueAcbList.contains(cbStat.getName())) {
                    ret.append("<li>Certified by " + cbStat.getName() + " - "
                            + getSuspendedDevelopersForAcb(edition2015,
                                    stats.getTotalDevsByCertifiedBodyWithListingsInEachCertificationStatusAndYear(),
                                    cbStat.getName())
                            + "</li>");
                    uniqueAcbList.add(cbStat.getName());
                    hasSuspended = true;
                }
            }
        }
        ret.append("</ul>");
        if (!hasSuspended) {
            ret.append("<ul><li>No certified bodies have suspended listings</li></ul>");
        }

        ret.append("<li>Total # of Developers with 2015 Listings (Regardless of Status) - "
                + stats.getTotalDevelopersWith2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalDevelopersByCertifiedBodyWithListingsEachYear()) {
            if (cbStat.getYear() == edition2015 && cbStat.getTotalDevelopersWithListings() > 0) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + cbStat.getTotalDevelopersWithListings() + "</li>");
            }
        }
        ret.append("</ul></ul>");
        return ret.toString();
    }

    private String createUniqueProductSection(final Statistics stats) {
        final int edition2014 = 2014;
        final int edition2015 = 2015;
        List<String> uniqueAcbList = new ArrayList<String>();
        Boolean hasSuspended = false;
        StringBuilder ret = new StringBuilder();

        ret
        .append("<h4>Total # of Certified Unique Products "
                + "(Regardless of Status or Edition - Including 2011) - "
                + stats.getTotalCertifiedProducts() + "</h4>");
        ret.append("<ul>");
        ret.append("<li>Total # of Unique Products with 2014 Listings (Regardless of Status) -  "
                + stats.getTotalCPs2014Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBody()) {
            if (cbStat.getYear() == edition2014 && cbStat.getTotalListings() > 0) {
                ret
                .append("<li>Certified by " + cbStat.getName() + " - " + cbStat.getTotalListings() + "</li>");
            }
        }
        ret.append("</ul>");

        ret.append("<li>Total # of Unique Products with Active 2014 Listings - "
                + stats.getTotalCPsActive2014Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus()) {
            if (!uniqueAcbList.contains(cbStat.getName())
                    && cbStat.getYear() == edition2014 && cbStat.getTotalListings() > 0
                    && (cbStat.getCertificationStatusName().equalsIgnoreCase("active"))) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getActiveCPsForAcb(edition2014,
                                stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus(),
                                cbStat.getName())
                        + "</li>");
                uniqueAcbList.add(cbStat.getName());
            }
        }
        ret.append("</ul>");

        uniqueAcbList.clear();
        hasSuspended = false;
        ret
        .append("<li>Total # of Unique Products with Suspended by ONC-ACB/Suspended by ONC 2014 Listings -  "
                + stats.getTotalCPsSuspended2014Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus()) {
            if (!uniqueAcbList.contains(cbStat.getName()) && cbStat.getYear().intValue() == edition2014
                    && cbStat.getTotalListings() > 0
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getSuspendedCPsForAcb(edition2014,
                                stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus(),
                                cbStat.getName())
                        + "</li>");
                uniqueAcbList.add(cbStat.getName());
                hasSuspended = true;
            }
        }
        ret.append("</ul>");
        if (!hasSuspended) {
            ret.append("<ul><li>No certified bodies have suspended listings</li></ul>");
        }

        ret.append("<li>Total # of Unique Products with 2015 Listings (Regardless of Status) -  "
                + stats.getTotalCPs2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBody()) {
            if (cbStat.getYear() == edition2015 && cbStat.getTotalListings() > 0) {
                ret
                .append("<li>Certified by " + cbStat.getName() + " - " + cbStat.getTotalListings() + "</li>");
            }
        }
        ret.append("</ul>");

        uniqueAcbList.clear();

        ret.append("<li>Total # of Unique Products with Active 2015 Listings -  "
                + stats.getTotalCPsActive2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus()) {
            if (!uniqueAcbList.contains(cbStat.getName())
                    && cbStat.getYear() == edition2015 && cbStat.getTotalListings() > 0
                    && (cbStat.getCertificationStatusName().equalsIgnoreCase("active"))) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getActiveCPsForAcb(edition2015,
                                stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus(),
                                cbStat.getName())
                        + "</li>");
                uniqueAcbList.add(cbStat.getName());
            }
        }
        ret.append("</ul>");

        uniqueAcbList.clear();
        ret.append("<li>Total # of Unique Products with Suspended by ONC-ACB/Suspended by ONC 2015 Listings -  "
                + stats.getTotalCPsSuspended2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus()) {
            if (!uniqueAcbList.contains(cbStat.getName())
                    && cbStat.getYear() == edition2015 && cbStat.getTotalListings() > 0
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + getSuspendedCPsForAcb(edition2015,
                                stats.getTotalCPListingsEachYearByCertifiedBodyAndCertificationStatus(),
                                cbStat.getName())
                        + "</li>");
                uniqueAcbList.add(cbStat.getName());
                hasSuspended = true;
            }
        }
        ret.append("</ul>");
        if (!hasSuspended) {
            ret.append("<ul><li>No certified bodies have suspended listings</li></ul>");
        }


        ret.append("<li>Total # of Unique Products with Active Listings (Regardless of Edition) - "
                + stats.getTotalCPsActiveListings() + "</ul></li>");
        ret.append("</ul>");
        return ret.toString();
    }

    private String createListingSection(final Statistics stats) {
        final int edition2014 = 2014;
        final int edition2015 = 2015;
        StringBuilder ret = new StringBuilder();

        ret.append(
                "<h4>Total # of Listings (Regardless of Status or Edition) -  " + stats.getTotalListings() + "</h4>");
        ret.append("<ul><li>Total # of Active (Including Suspended by ONC/ONC-ACB 2014 Listings) - "
                + stats.getTotalActive2014Listings() + "</li>");

        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalActiveListingsByCertifiedBody()) {
            if (cbStat.getYear() == edition2014 && cbStat.getTotalListings() > 0) {
                ret
                .append("<li>Certified by " + cbStat.getName() + " - " + cbStat.getTotalListings() + "</li>");
            }
        }
        ret.append("</ul>");

        ret.append("<li>Total # of Active (Including Suspended by ONC/ONC-ACB 2015 Listings) - "
                + stats.getTotalActive2015Listings() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyStatistics cbStat : stats.getTotalActiveListingsByCertifiedBody()) {
            if (cbStat.getYear() == edition2015 && cbStat.getTotalListings() > 0) {
                ret
                .append("<li>Certified by " + cbStat.getName() + " - " + cbStat.getTotalListings() + "</li>");
            }
        }
        ret.append("</ul>");

        Boolean hasOtherTest = false;
        ret.append("<li>Total # of 2015 Listings with Alternative Test Methods -  "
                + stats.getTotalListingsWithAlternativeTestMethods() + "</li>");
        ret.append("<ul>");
        for (CertifiedBodyAltTestStatistics cbStat
                : stats.getTotalListingsWithCertifiedBodyAndAlternativeTestMethods()) {
            if (cbStat.getTotalListings() > 0) {
                ret.append("<li>Certified by " + cbStat.getName() + " - "
                        + cbStat.getTotalListings()
                        + "</li>");
                hasOtherTest = true;
            }
        }
        if (!hasOtherTest) {
            ret.append("<li>No listings have Alternative Test Methods</li>");
        }
        ret.append("</ul>");

        ret.append(
                "<li>Total # of 2014 Listings (Regardless of Status) - " + stats.getTotal2014Listings() + "</li>");
        ret.append(
                "<li>Total # of 2015 Listings (Regardless of Status) - " + stats.getTotal2015Listings() + "</li>");
        ret.append(
                "<li>Total # of 2011 Listings (Regardless of Status) - " + stats.getTotal2011Listings() + "</li></ul>");
        return ret.toString();

    }

    private void setAsynchronousStatisticsInitializor(
            final AsynchronousStatisticsInitializor asynchronousStatisticsInitializor) {
        this.asynchronousStatisticsInitializor = asynchronousStatisticsInitializor;
    }

    private Long getSuspendedDevelopersForAcb(
            final Integer year, final List<CertifiedBodyStatistics> cbStats, final String acb) {
        Long count = 0L;
        for (CertifiedBodyStatistics cbStat : cbStats) {
            if (cbStat.getYear().equals(year) && cbStat.getName().equalsIgnoreCase(acb)
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                count = count + cbStat.getTotalDevelopersWithListings();
            }
        }
        return count;
    }

    private Long getActiveDevelopersForAcb(
            final Integer year, final List<CertifiedBodyStatistics> cbStats, final String acb) {
        Long count = 0L;
        for (CertifiedBodyStatistics cbStat : cbStats) {
            if (cbStat.getYear().equals(year) && cbStat.getName().equalsIgnoreCase(acb)
                    && (cbStat.getCertificationStatusName().toLowerCase().equalsIgnoreCase("active"))) {
                count = count + cbStat.getTotalDevelopersWithListings();
            }
        }
        return count;
    }

    private Long getActiveCPsForAcb(final Integer year, final List<CertifiedBodyStatistics> cbStats, final String acb) {
        Long count = 0L;
        for (CertifiedBodyStatistics cbStat : cbStats) {
            if (cbStat.getYear().equals(year) && cbStat.getName().equalsIgnoreCase(acb)
                    && (cbStat.getCertificationStatusName().toLowerCase().equalsIgnoreCase("active"))) {
                count = count + cbStat.getTotalListings();
            }
        }
        return count;
    }

    private Long getSuspendedCPsForAcb(
            final Integer year, final List<CertifiedBodyStatistics> cbStats, final String acb) {
        Long count = 0L;
        for (CertifiedBodyStatistics cbStat : cbStats) {
            if (cbStat.getYear().equals(year) && cbStat.getName().equalsIgnoreCase(acb)
                    && cbStat.getCertificationStatusName().toLowerCase().contains("suspended")) {
                count = count + cbStat.getTotalListings();
            }
        }
        return count;
    }

    public NotificationDAO getNotificationDao() {
        return notificationDao;
    }

    public void setNotificationDao(final NotificationDAO notificationDao) {
        this.notificationDao = notificationDao;
    }
}

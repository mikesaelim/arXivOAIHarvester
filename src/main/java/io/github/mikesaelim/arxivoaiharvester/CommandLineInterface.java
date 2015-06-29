package io.github.mikesaelim.arxivoaiharvester;

import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleMetadata;
import io.github.mikesaelim.arxivoaiharvester.model.data.ArticleVersion;
import io.github.mikesaelim.arxivoaiharvester.model.request.*;
import io.github.mikesaelim.arxivoaiharvester.model.response.GetRecordResponse;
import io.github.mikesaelim.arxivoaiharvester.model.response.ListRecordsResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Command line interface, showing an example of using the arXiv OAI Harvester.  It accepts the parameters for one query
 * and prints the results to the console.
 */
public class CommandLineInterface {

    public static void main(String[] args) throws InterruptedException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        ArxivOAIHarvester harvester = new ArxivOAIHarvester(httpClient);
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("Welcome to the command line interface of the arXiv OAI Harvester!");
        System.out.println("This program sends one query to the arXiv OAI repository and prints the results to STDOUT.");
        System.out.println("It uses the default settings for query retries.");
        System.out.println();
        System.out.println("Which verb would you like to use?");
        System.out.println("    1) GetRecord - retrieve a single record");
        System.out.println("    2) ListRecords - retrieve a range of records");
        System.out.println("    or enter anything else to quit.");

        switch (scanner.nextLine().trim()) {
            case "1":
                handleGetRecordRequest(scanner, harvester);
                break;
            case "2":
                handleListRecordsRequest(scanner, harvester);
        }
    }



    /**
     * Handle "GetRecord" requests.
     */
    private static void handleGetRecordRequest(Scanner scanner, ArxivOAIHarvester harvester) {
        System.out.println("Sweet, let's do a GetRecord query.");
        System.out.println();

        GetRecordRequest request = constructGetRecordRequest(scanner);
        System.out.println();

        System.out.println("Sending GetRecord request, waiting for response...");
        GetRecordResponse response = harvester.harvest(request);

        System.out.println("Response received!");
        printLine("    Response datetime: ", response.getResponseDate());
        if (response.getRecord() != null) {
            System.out.println("************ Record 1 of 1 ************");
            printRecord(response.getRecord());
            System.out.println("***************************************");
        } else {
            System.out.println("    No record found.");
        }
    }

    /**
     * Handle "ListRecords" requests.
     */
    private static void handleListRecordsRequest(Scanner scanner, ArxivOAIHarvester harvester) {
        System.out.println("Sweet, let's do a ListRecords query.");
        System.out.println();

        ListRecordsRequest request = constructListRecordsRequest(scanner);
        System.out.println();

        while (request != ListRecordsRequest.NONE) {
            System.out.println("Sending ListRecords request, waiting for response......");
            ListRecordsResponse response = harvester.harvest(request);

            boolean shouldContinueLoop = handleListRecordResponse(scanner, response);
            if (!shouldContinueLoop) break;

            request = response.resumption();
        }
    }



    /**
     * Construct a "GetRecord" request from user inputs.
     */
    private static GetRecordRequest constructGetRecordRequest(Scanner scanner) {
        System.out.println("What is the identifier for the record you wish to get?  " +
                "For example, \"oai:arXiv.org:1302.2146\".");

        while (true) {
            String identifier = scanner.nextLine().trim();

            if (isBlank(identifier)) {
                System.out.println("    Please enter something.  Try again?");
            } else {
                try {
                    return new GetRecordRequest(identifier);
                } catch (URISyntaxException e) {
                    System.out.println("    Sorry, identifier \"" + identifier + "\" did not result in a valid " +
                            "request URI.  Try again?");
                }
            }
        }
    }

    /**
     * Construct a "ListRecords" request from user inputs.
     */
    private static ListRecordsRequest constructListRecordsRequest(Scanner scanner) {
        while (true) {
            System.out.println("From date?  (in yyyy-mm-dd format; leave it blank for none)");
            String fromDate = scanner.nextLine().trim();
            System.out.println("Until date?  (in yyyy-mm-dd format; leave it blank for none)");
            String untilDate = scanner.nextLine().trim();
            System.out.println("Set restriction?  (leave it blank for none)");
            String setSpec = scanner.nextLine().trim();

            try {
                return new ListRecordsRequest(
                        isBlank(fromDate) ? null : LocalDate.parse(fromDate),
                        isBlank(untilDate) ? null : LocalDate.parse(untilDate),
                        isBlank(setSpec) ? null : setSpec);
            } catch (DateTimeParseException e) {
                System.out.println("    Sorry, one of the dates was not a valid date string.  Try again?");
            } catch (URISyntaxException e) {
                System.out.println(    "Sorry, the inputs did not result in a valid request URI.  Try again?");
            }
        }
    }



    /**
     * Handle a single response to a "ListRecords" request.
     */
    private static boolean handleListRecordResponse(Scanner scanner, ListRecordsResponse response) {
        System.out.println("Response received!");
        printLine("    Response datetime: ", response.getResponseDate());
        List<ArticleMetadata> records = response.getRecords();
        if (records.size() == 0) {
            System.out.println("    No records found.");
            return true;  // No records found, but there still could be a legal resumption token returned.
        }
        printLine("    Number of records retrieved in this batch: ", records.size());
        if (response.hasResumption()) {
            System.out.println("    Resumption token returned - there is another batch of records after this one.");
        }
        printLine("    Current cursor position: ", response.getCursor());
        printLine("    Complete list size: ", response.getCompleteListSize());
        System.out.println();

        System.out.println("Now you can view each of the records in this batch individually.  Simply press ENTER to " +
                "bring up the next record.  If there is another batch after this one, the harvester will make a " +
                "request to retrieve it when you get to the end.  Type \'q\' and ENTER to exit.");
        for (int recordNumber = 0; recordNumber < records.size(); recordNumber+=999) {
            String readerInput = scanner.nextLine();
            if ("q".equals(readerInput.trim())) {
                return false;
            }

            System.out.println("************ Record " + (recordNumber + 1) + " of " + records.size() + " ************");
            printRecord(records.get(recordNumber));
            System.out.println("***************************************");
            System.out.println();
        }

        if (!response.hasResumption()) {
            System.out.println("End of records.");
        } else {
            System.out.println("End of records, but there is another batch after this.  Press ENTER to send the " +
                    "resumption request.  Type \'q\' and ENTER to exit.");
            String readerInput = scanner.nextLine();
            if ("q".equals(readerInput.trim())) {
                return false;
            }
        }

        return true;
    }



    /**
     * This prints a single record from the response.
     */
    private static void printRecord(ArticleMetadata record) {
        printLine("Retrieval datetime: ", record.getRetrievalDateTime());
        System.out.println();

        printLine("Identifier: ", record.getIdentifier());
        printLine("Datestamp: ", record.getDatestamp());
        System.out.print("Sets:");
        for (String setSpec : record.getSets()) {
            System.out.print(" " + setSpec);
        }
        System.out.println();
        if (record.isDeleted()) {
            System.out.println("Deleted");
            return;
        }

        printLine("Id: ", record.getId());
        printLine("Submitter: ", record.getSubmitter());
        System.out.println();

        for (ArticleVersion version : record.getVersions()) {
            System.out.println("v" + version.getVersionNumber() + ":");
            printLine("   Submission time: ", version.getSubmissionTime());
            printLine("   Size: ", version.getSize());
            printLine("   Source type: ", version.getSourceType());
        }
        System.out.println();

        printLine("Title: ", record.getTitle());
        printLine("Authors: ", record.getAuthors());
        System.out.print("Categories:");
        for (String category : record.getCategories()) {
            System.out.print(" " + category);
        }
        System.out.println();
        printLine("Comments: ", record.getComments());
        printLine("Proxy: ", record.getProxy());
        printLine("Report No.: ", record.getReportNo());
        printLine("ACM class: ", record.getAcmClass());
        printLine("MSC class: ", record.getMscClass());
        printLine("Journal ref: ", record.getJournalRef());
        printLine("DOI: ", record.getDoi());
        printLine("License: ", record.getLicense());
        printLine("Abstract: ", record.getArticleAbstract());
    }

    /**
     * This prints a line, unless the data is null.
     */
    private static void printLine(String description, Object value) {
        if (value != null) {
            System.out.println(description + value.toString());
        }
    }



}

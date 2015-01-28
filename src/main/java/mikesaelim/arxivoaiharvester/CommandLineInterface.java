package mikesaelim.arxivoaiharvester;

import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.data.ArticleVersion;
import mikesaelim.arxivoaiharvester.io.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 *
 * Created by Mike Saelim on 1/11/15.
 */
public class CommandLineInterface {

    public static void main(String[] args) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("Welcome to the command line interface of the arXiv OAI Harvester!");
        System.out.println("This program sends one query to the arXiv OAI repository and prints out the results.");
        System.out.println();
        System.out.println("Which verb would you like to use?");
        System.out.println("    1) GetRecord");
        System.out.println("    2) ListRecords");
        System.out.println("    or enter anything else to quit.");

        ArxivRequest request;
        String verbChoice = scanner.nextLine();
        try {
            switch (verbChoice.trim()) {
                case "1":
                    request = getGetRecordRequest(scanner);
                    break;
                case "2":
                    request = getListRecordsRequest(scanner);
                    break;
                default:
                    return;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return;
        }
        System.out.println();
        System.out.println("Preparing harvester...");
        ArxivOAIHarvester harvester;
        try {
            harvester = new ArxivOAIHarvester(httpClient, request);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println("Sending query; retrieving response...");
        ArxivResponse response;
        try {
            response = harvester.getNextBatch();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return;
        }

        System.out.println();
        System.out.println("Response received!");
        printLine("    Response datetime: ", response.getResponseDate());
        List<ArticleMetadata> records = response.getRecords();
        if (records == null) {
            System.out.println("Blank response.");
            return;
        } else if (records.size() == 0) {
            System.out.println("No records returned.");
            return;
        } else {
            printLine("    Number of records retrieved in this batch: ", records.size());
            System.out.println();
        }

        System.out.println("Current state of the harvester:");
        printLine("    Current position: ", harvester.getCurrentPosition());
        printLine("    Complete list size: ", harvester.getCompleteSize());
        System.out.println();


        System.out.println("Now you can view each of the records individually.  Simply press ENTER to bring up the next record.  Type \'q\' and ENTER to exit.");
        for (int recordNumber = 0; recordNumber < records.size(); recordNumber++) {
            String readerInput = scanner.nextLine();
            if (readerInput.trim().equals("q")) {
                return;
            }

            System.out.println("************ Record " + (recordNumber + 1) + " of " + records.size() + " ************");
            printRecord(records.get(recordNumber));
            System.out.println();

            if (recordNumber == records.size() - 1) {
                System.out.println("************************************");
                System.out.println("End of records.");
                return;
            }
        }
    }

    /**
     * This handles the creation of an ArxivRequest for the "GetRecord" verb.
     */
    private static ArxivGetRecordRequest getGetRecordRequest(Scanner scanner) throws URISyntaxException {
        System.out.println("Sweet, let's do a GetRecord query.");
        System.out.println();
        System.out.println("What is the identifier for the record you wish to get?");

        String identifier = scanner.nextLine().trim();

        return new ArxivGetRecordRequest(identifier);
    }

    /**
     * This handles the creation of an ArxivRequest for the "ListRecords" verb.
     */
    private static ArxivListRecordsRequest getListRecordsRequest(Scanner scanner) throws URISyntaxException {
        System.out.println("Sweet, let's do a ListRecords query.");
        System.out.println();
        System.out.println("From date?  (in yyyy-mm-dd format; leave it blank for none)");
        String fromDate = scanner.nextLine().trim();
        System.out.println("Until date?  (in yyyy-mm-dd format; leave it blank for none)");
        String untilDate = scanner.nextLine().trim();
        System.out.println("Set restriction?  (leave it blank for none)");
        String setSpec = scanner.nextLine().trim();

        return new ArxivListRecordsRequest(
                fromDate.isEmpty() ? null : LocalDate.parse(fromDate),
                untilDate.isEmpty() ? null : LocalDate.parse(untilDate),
                setSpec.isEmpty() ? null : setSpec);
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

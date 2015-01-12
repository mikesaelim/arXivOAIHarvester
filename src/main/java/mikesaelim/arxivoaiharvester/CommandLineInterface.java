package mikesaelim.arxivoaiharvester;

import mikesaelim.arxivoaiharvester.data.ArticleMetadata;
import mikesaelim.arxivoaiharvester.io.*;

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
        String verbChoice = scanner.next();
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
        ArxivOAIHarvester harvester = new ArxivOAIHarvester(request);

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
        System.out.println("    Response datetime: " + response.getResponseDate().toString());
        if (response.getResumptionToken() != null) {
            System.out.println("    Resumption token: " + response.getResumptionToken());
            System.out.println("    Cursor: " + response.getCursor());
            System.out.println("    Complete list size: " + response.getCompleteListSize());
        }
        List<ArticleMetadata> records = response.getRecords();
        System.out.println("    Number of records retrieved in this batch: " + records.size());
        System.out.println();
        if (records.size() == 0) {
            return;
        }
        System.out.println("Now you can view each of the records individually.  Simply press ENTER to bring up the next record.  Type \'q\' and ENTER to exit.");
        for (int recordNumber = 0; recordNumber < records.size(); recordNumber++) {
            System.out.println("************ Record " + (recordNumber + 1) + " of " + records.size() + " ************");
            printRecord(records.get(recordNumber));
            System.out.println();
        }
        System.out.println("End of records.");
    }

    private static ArxivGetRecordRequest getGetRecordRequest(Scanner scanner) throws URISyntaxException {
        System.out.println("Sweet, let's do a GetRecord query.");
        System.out.println();
        System.out.println("What is the identifier for the record you wish to get?");

        String identifier = scanner.next().trim();

        return new ArxivGetRecordRequest(identifier);
    }

    private static ArxivListRecordsRequest getListRecordsRequest(Scanner scanner) throws URISyntaxException {
        System.out.println("Sweet, let's do a ListRecords query.");
        System.out.println();
        System.out.println("From date?  (in yyyy-mm-dd format; leave it blank for none)");
        String fromDate = scanner.next().trim();
        System.out.println("Until date?  (in yyyy-mm-dd format; leave it blank for none)");
        String untilDate = scanner.next().trim();
        System.out.println("Set restriction?  (leave it blank for none)");
        String setSpec = scanner.next().trim();

        return new ArxivListRecordsRequest(
                fromDate.isEmpty() ? null : LocalDate.parse(fromDate),
                untilDate.isEmpty() ? null : LocalDate.parse(untilDate),
                setSpec.isEmpty() ? null : setSpec);
    }

    private static void printRecord(ArticleMetadata record) {

    }



}

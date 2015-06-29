arXiv OAI Harvester
=================

Copyright (c) 2015 Mike Saelim, mike.saelim@gmail.com

[**Current version**](http://semver.org/): 0.0.0

This is an OAI Harvester library for the arXiv preprint repository, written in Java.  At this point, it is still a work
in progress.

Upon completion, this library will allow its users to query the arXiv OAI repository for items in the "arXivRaw" metadata
format, giving them access to all the metadata of all the articles on the arXiv.

Informational links:

* [arXiv](http://arxiv.org/)
* [arXiv OAI interface](http://arxiv.org/help/oa/index)
* [OAI-PMH: Open Archives Initiative - Protocol for Metadata Harvesting](http://www.openarchives.org/pmh/)
    * [Formal definition of the OAI protocol](http://www.openarchives.org/OAI/openarchivesprotocol.html)


####Covering my ass

I mostly designed this library to support a future larger project of mine, and for educational purposes to give me more
practice coding and designing.  No guarantees on current reliability and future support!  If you prioritize these things,
I might suggest using one of the more general Java OAI Harvesters out there, which (hopefully) have reached some kind of
stability.

##Usage

I strongly recommend reading up on the above links before using this library, because this library will not insulate you
from all the peculiarities of the OAI protocol.  

The API is designed so that users construct a central harvester object, and then pass GetRecord and ListRecords requests
to it.  The harvester issues the appropriate HTTP calls to the arXiv OAI repository, parses the result, and outputs the
response to the user.  Responses contain all the data contained in the "arXivRaw" metadata format.  

It will probably take at least a few seconds, and possibly minutes, to process each request.  This is because a lot of
data may be transmitted by the repository, and because the repository throttles requests when they are coming in too
fast.  Both of these reasons are out of the control of the harvester.  The harvester does have some parameters you can
set to control how long it is allowed to wait before retrying, and how many times it will retry.  This also means that
harvesting should be thought of as more of a bulk-retrieval-once-a-day-to-a-local-database thing, and not thought of as
a retrieval-on-demand thing.

**A single harvester should be used for all of the requests, and this harvester should only be used by a single thread.**  
Currently, the implementation of the harvester is blocking and not thread-safe.  Furthermore, because the arXiv OAI
repository throttles your requests based on your IP, multiple harvesters or threads will end up blocking each other 
anyway.  In the future, I may rewrite the implementation to queue multiple requests and resolve them asynchronously.

###Importing the library

At this point, while the library is still in development, you can just clone the repo, build a jar, and throw that jar
into your project.  Once this is ready for release, I will probably host the jar(s) somewhere.

###Preparing the harvester

The simplest preparation is to pass a `CloseableHttpClient` into the constructor:

    CloseableHttpClient httpClient = HttpClients.createDefault();  
    ArxivOAIHarvester harvester = new ArxivOAIHarvester(httpClient);
    
This will construct a harvester with the default settings for three important flow control parameters:

* the maximum number of retries: 3,
* the minimum wait time between retries: 10 seconds, and
* the maximum wait time between retries: 5 minutes.

These are needed because the repository can respond to requests with a 503 Retry-After response, which says "Yo, either
I'm super busy right now or you've been requesting too frequently, chill for X seconds and try again."  And if you don't
wait, you'll probably get another 503 Retry-After and have to wait longer.  The default harvester will respect the 
repository's management of the flow control by retrying only 3 times, restricting its requests to no faster than 10
seconds between requests, and timing out if the repository requests a wait longer than 5 minutes.  But you can set these
values yourself - for example, for 5 retries between 20 seconds and 30 minutes,

    CloseableHttpClient httpClient = HttpClients.createDefault();
    ArxivOAIHarvester harvester = new ArxivOAIHarvester(httpClient, 5, Duration.ofSeconds(20), Duration.ofMinutes(30));
    
It is also suggested that you supply "User-Agent" and "From" HTTP headers to identify to the repository who you are:

    harvester.setUserAgentHeader("Dave's Super Curious Bot, v0.1");
    harvester.setFromHeader("dave@daves.io");

###Retrieving a single record from the repository

To retrieve a single record by its identifier, construct a `GetRecordRequest` and pass it into the harvester:

    GetRecordRequest request = new GetRecordRequest("oai:arXiv.org:1302.2146");
    GetRecordResponse response = harvester.harvest(request);
    ArticleMetadata record = response.getRecord();
    
The identifier string will always be "oai:arXiv.org:" followed by the arXiv identifier you're probably more used to, so
you can even leave the "oai:arXiv.org:" part off.  See arXiv's pages about the OAI for more information.

The resulting `GetRecordResponse` will contain the record, if it exists, as an `ArticleMetadata` object.  If no record
by that identifier was found, then `response.getRecord()` will return null.  If there are any issues sending the 
request, receiving the response, or parsing the response, the harvester will throw a runtime exception or error - see 
the javadoc for `ArxivOAIHarvester` for a full list.

###Retrieving a range of records from the repository

To retrieve a range of records between two dates, and/or of a specific set, construct a `ListRecordsRequest` and pass it
into the harvester.  Because many records may be returned, the repository will page the results (usually the pages are
in sets of 1000 records), and you'll need to issue a new resumption request for each page.  But the API takes care of
the resumption token stuff for you, if you follow the recommended pattern:

    ListRecordsRequest request = new ListRecordsRequest(LocalDate.of(2015, 6, 29), null, "physics:hep-ph");
    while (request != ListRecordsRequest.NONE) {
        ListRecordsResponse response = harvester.harvest(request);
        List<ArticleMetadata> records = response.getRecords();
        // do whatever
        request = response.resumption();
    }

The request takes three optional parameters: two `LocalDate` objects specifying the range, and the set to restrict the
search to.  The date does not refer to the original submission date of the article, but rather the last time the OAI 
record for that article was updated.  Thus, a request to retrieve all the records between two dates will not necessarily
grab all the articles submitted between those dates.  The list of sets that the arXiv repository supports can be found
[here](http://export.arxiv.org/oai2?verb=ListSets).  See arXiv's pages about the OAI for more information.

The resulting `ListRecordsResponse` objects will contain a list of records, if they exist, as `ArticleMetadata` objects.
If no records were found in that range and set, then `response.getRecords()` will return an empty list.  If there are 
any issues sending the request, receiving the response, or parsing the response, the harvester will throw a runtime 
exception or error - see the javadoc for `ArxivOAIHarvester` for a full list.

## Try it out!

I've supplied a `CommandLineInterface` class with an executable `main()` method that executes one request to the arXiv
OAI repository.  You can run this either by building the jar and running it, or by typing

    gradle clean run
    
in the repository directory.  This should give you a feel for how it all behaves.


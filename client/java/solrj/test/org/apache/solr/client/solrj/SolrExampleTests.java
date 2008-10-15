/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.client.solrj;


import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.request.LukeRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.LukeResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.XML;
import org.apache.solr.common.params.FacetParams;

/**
 * This should include tests against the example solr config
 * 
 * This lets us try various SolrServer implementations with the same tests.
 * 
 * @version $Id$
 * @since solr 1.3
 */
abstract public class SolrExampleTests extends SolrExampleTestBase 
{
  /**
   * query the example
   */
  public void testExampleConfig() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    
    // Now add something...
    SolrInputDocument doc = new SolrInputDocument();
    String docID = "1112211111";
    doc.addField( "id", docID, 1.0f );
    doc.addField( "name", "my name!", 1.0f );
    
    Assert.assertEquals( null, doc.getField("foo") );
    Assert.assertTrue(doc.getField("name").getValue() != null );
        
    UpdateResponse upres = server.add( doc ); 
    System.out.println( "ADD:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    upres = server.commit( true, true );
    System.out.println( "COMMIT:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    upres = server.optimize( true, true );
    System.out.println( "OPTIMIZE:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    SolrQuery query = new SolrQuery();
    query.setQuery( "id:"+docID );
    QueryResponse response = server.query( query );
    
    Assert.assertEquals(docID, response.getResults().get(0).getFieldValue("id") );
    
    // Now add a few docs for facet testing...
    List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField( "id", "2", 1.0f );
    doc2.addField( "inStock", true, 1.0f );
    doc2.addField( "price", 2, 1.0f );
    doc2.addField( "timestamp", new java.util.Date(), 1.0f );
    docs.add(doc2);
    SolrInputDocument doc3 = new SolrInputDocument();
    doc3.addField( "id", "3", 1.0f );
    doc3.addField( "inStock", false, 1.0f );
    doc3.addField( "price", 3, 1.0f );
    doc3.addField( "timestamp", new java.util.Date(), 1.0f );
    docs.add(doc3);
    SolrInputDocument doc4 = new SolrInputDocument();
    doc4.addField( "id", "4", 1.0f );
    doc4.addField( "inStock", true, 1.0f );
    doc4.addField( "price", 4, 1.0f );
    doc4.addField( "timestamp", new java.util.Date(), 1.0f );
    docs.add(doc4);
    SolrInputDocument doc5 = new SolrInputDocument();
    doc5.addField( "id", "5", 1.0f );
    doc5.addField( "inStock", false, 1.0f );
    doc5.addField( "price", 5, 1.0f );
    doc5.addField( "timestamp", new java.util.Date(), 1.0f );
    docs.add(doc5);
    
    upres = server.add( docs ); 
    System.out.println( "ADD:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    upres = server.commit( true, true );
    System.out.println( "COMMIT:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    upres = server.optimize( true, true );
    System.out.println( "OPTIMIZE:"+upres.getResponse() );
    Assert.assertEquals(0, upres.getStatus());
    
    query = new SolrQuery("*:*");
    query.addFacetQuery("price:[* TO 2]");
    query.addFacetQuery("price:[2 TO 4]");
    query.addFacetQuery("price:[5 TO *]");
    query.addFacetField("inStock");
    query.addFacetField("price");
    query.addFacetField("timestamp");
    query.removeFilterQuery("inStock:true");
    
    response = server.query( query );
    Assert.assertEquals(0, response.getStatus());
    Assert.assertEquals(5, response.getResults().getNumFound() );
    Assert.assertEquals(3, response.getFacetQuery().size());    
    Assert.assertEquals(2, response.getFacetField("inStock").getValueCount());
    Assert.assertEquals(4, response.getFacetField("price").getValueCount());
    
    // test a second query, test making a copy of the main query
    SolrQuery query2 = query.getCopy();
    query2.addFilterQuery("inStock:true");
    response = server.query( query2 );
    Assert.assertEquals(1, query2.getFilterQueries().length);
    Assert.assertEquals(0, response.getStatus());
    Assert.assertEquals(2, response.getResults().getNumFound() );
    Assert.assertFalse(query.getFilterQueries() == query2.getFilterQueries());
  }


  /**
   * query the example
   */
  public void testAddRetrieve() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    
    // Now add something...
    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField( "id", "id1", 1.0f );
    doc1.addField( "name", "doc1", 1.0f );
    doc1.addField( "price", 10 );

    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField( "id", "id2", 1.0f );
    doc2.addField( "name", "doc2", 1.0f );
    doc2.addField( "price", 20 );
    
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
    docs.add( doc1 );
    docs.add( doc2 );
    
    // Add the documents
    server.add( docs );
    server.commit();
    
    SolrQuery query = new SolrQuery();
    query.setQuery( "*:*" );
    query.addSortField( "price", SolrQuery.ORDER.asc );
    QueryResponse rsp = server.query( query );
    
    Assert.assertEquals( 2, rsp.getResults().getNumFound() );
    System.out.println( rsp.getResults() );
    
    // Now do it again
    server.add( docs );
    server.commit();
    
    rsp = server.query( query );
    Assert.assertEquals( 2, rsp.getResults().getNumFound() );
    System.out.println( rsp.getResults() );
    
  }
  
  /**
   * query the example
   */
  public void testCommitWithin() throws Exception
  {    
    // make sure it is empty...
    SolrServer server = getSolrServer();
    server.deleteByQuery( "*:*" );// delete everything!
    server.commit();
    QueryResponse rsp = server.query( new SolrQuery( "*:*") );
    Assert.assertEquals( 0, rsp.getResults().getNumFound() );

    // Now try a timed commit...
    SolrInputDocument doc3 = new SolrInputDocument();
    doc3.addField( "id", "id3", 1.0f );
    doc3.addField( "name", "doc3", 1.0f );
    doc3.addField( "price", 10 );
    UpdateRequest up = new UpdateRequest();
    up.add( doc3 );
    up.setCommitWithin( 10 );
    up.process( server );
    
    rsp = server.query( new SolrQuery( "*:*") );
    Assert.assertEquals( 0, rsp.getResults().getNumFound() );
    
    Thread.sleep( 500 ); // wait 1/2 seconds...

    // now check that it comes out...
    rsp = server.query( new SolrQuery( "id:id3") );
    Assert.assertEquals( 1, rsp.getResults().getNumFound() );
  }
  
  
  protected void assertNumFound( String query, int num ) throws SolrServerException, IOException
  {
    QueryResponse rsp = getSolrServer().query( new SolrQuery( query ) );
    if( num != rsp.getResults().getNumFound() ) {
      fail( "expected: "+num +" but had: "+rsp.getResults().getNumFound() + " :: " + rsp.getResults() );
    }
  }

  public void testAddDelete() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    
    SolrInputDocument[] doc = new SolrInputDocument[3];
    for( int i=0; i<3; i++ ) {
      doc[i] = new SolrInputDocument();
      doc[i].setField( "id", i + " & 222", 1.0f );
    }
    String id = (String) doc[0].getField( "id" ).getFirstValue();
    
    server.add( doc[0] );
    server.commit();
    assertNumFound( "*:*", 1 ); // make sure it got in
    
    // make sure it got in there
    server.deleteById( id );
    server.commit();
    assertNumFound( "*:*", 0 ); // make sure it got out
    
    // add it back 
    server.add( doc[0] );
    server.commit();
    assertNumFound( "*:*", 1 ); // make sure it got in
    server.deleteByQuery( "id:\""+ClientUtils.escapeQueryChars(id)+"\"" );
    server.commit();
    assertNumFound( "*:*", 0 ); // make sure it got out
    
    // Add two documents
    for( SolrInputDocument d : doc ) {
      server.add( d );
    }
    server.commit();
    assertNumFound( "*:*", 3 ); // make sure it got in
    
    // should be able to handle multiple delete commands in a single go
    StringWriter xml = new StringWriter();
    xml.append( "<delete>" );
    for( SolrInputDocument d : doc ) {
      xml.append( "<id>" );
      XML.escapeCharData( (String)d.getField( "id" ).getFirstValue(), xml );
      xml.append( "</id>" );
    }
    xml.append( "</delete>" );
    DirectXmlRequest up = new DirectXmlRequest( "/update", xml.toString() );
    server.request( up );
    server.commit();
    assertNumFound( "*:*", 0 ); // make sure it got out
  }
  
  public void testLukeHandler() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    
    SolrInputDocument[] doc = new SolrInputDocument[5];
    for( int i=0; i<doc.length; i++ ) {
      doc[i] = new SolrInputDocument();
      doc[i].setField( "id", "ID"+i, 1.0f );
      server.add( doc[i] );
    }
    server.commit();
    assertNumFound( "*:*", doc.length ); // make sure it got in
    
    LukeRequest luke = new LukeRequest();
    luke.setShowSchema( false );
    LukeResponse rsp = luke.process( server );
    assertNull( rsp.getFieldTypeInfo() ); // if you don't ask for it, the schema is null
    
    luke.setShowSchema( true );
    rsp = luke.process( server );
    assertNotNull( rsp.getFieldTypeInfo() ); 
  }
  

  public void testPingHandler() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    server.commit();
    assertNumFound( "*:*", 0 ); // make sure it got in
    
    // should be ok
    server.ping();
    
    try {
      SolrPing ping = new SolrPing();
      ping.getParams().set( "qt", "unknown handler!" );
      ping.process( server );
      fail( "sent unknown query type!" );
    }
    catch( Exception ex ) {
      // expected
    }
  }
  
  public void testFaceting() throws Exception
  {    
    SolrServer server = getSolrServer();
    
    // Empty the database...
    server.deleteByQuery( "*:*" );// delete everything!
    server.commit();
    assertNumFound( "*:*", 0 ); // make sure it got in
    
    ArrayList<SolrInputDocument> docs = new ArrayList<SolrInputDocument>(10);
    for( int i=1; i<=10; i++ ) {
      SolrInputDocument doc = new SolrInputDocument();
      doc.setField( "id", i+"", 1.0f );
      if( (i%2)==0 ) {
        doc.addField( "features", "two" );
      }
      if( (i%3)==0 ) {
        doc.addField( "features", "three" );
      }
      if( (i%4)==0 ) {
        doc.addField( "features", "four" );
      }
      if( (i%5)==0 ) {
        doc.addField( "features", "five" );
      }
      docs.add( doc );
    }
    server.add( docs );
    server.commit();
    
    SolrQuery query = new SolrQuery( "*:*" );
    query.remove( FacetParams.FACET_FIELD );
    query.addFacetField( "features" );
    query.setFacetMinCount( 0 );
    query.setFacet( true );
    query.setRows( 0 );
    
    QueryResponse rsp = server.query( query );
    assertEquals( docs.size(), rsp.getResults().getNumFound() );
    
    List<FacetField> facets = rsp.getFacetFields();
    assertEquals( 1, facets.size() );
    FacetField ff = facets.get( 0 );
    assertEquals( "features", ff.getName() );
    System.out.println( "111: "+ff.getValues() );
    // check all counts
    assertEquals( "[two (5), three (3), five (2), four (2)]", ff.getValues().toString() );
    
    // should be the same facets with minCount=0
    query.setFilterQueries( "features:two" );
    rsp = server.query( query );
    ff = rsp.getFacetField( "features" );
    assertEquals( "[two (5), four (2), five (1), three (1)]", ff.getValues().toString() );
    
    // with minCount > 3
    query.setFacetMinCount( 4 );
    rsp = server.query( query );
    ff = rsp.getFacetField( "features" );
    assertEquals( "[two (5)]", ff.getValues().toString() );

    // with minCount > 3
    query.setFacetMinCount( -1 );
    rsp = server.query( query );
    ff = rsp.getFacetField( "features" );
    
    System.out.println( rsp.getResults().getNumFound() + " :::: 444: "+ff.getValues() );
  }
}
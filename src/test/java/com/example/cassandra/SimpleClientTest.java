package com.example.cassandra;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

/**
 * Class to test a simple Cassandra client using Datastax driver.
 * 
 * The tests consider a test cluster with 3 nodes, with the following IP
 * addresses: - 127.0.0.1 - 127.0.0.2 - 127.0.0.3
 * 
 * One of the possible tests is to shut down the node in 127.0.0.1. The
 * connection will still be successful, but the connection and disconnection
 * times will considerably increase.
 * 
 * @author pfernandom - Pedro Marquez
 * 
 */
public class SimpleClientTest {
	private SimpleClient client;
	private long start;
	private long end;

	@Rule
	public TestName name = new TestName();

	@Before
	public void setUp() {
		client = new SimpleClient();
		client.connect("musicdb", "127.0.0.1", "127.0.0.2");
		start = System.currentTimeMillis();
	}

	@After
	public void tearDown() {
		long middleEnd = System.currentTimeMillis();
		System.err.println("DEBUG: Test " + name.getMethodName() + " took " + (middleEnd - start)
				+ " MilliSeconds");
		client.close();
		end = System.currentTimeMillis();
		System.err.println("DEBUG: Closing connection " + name.getMethodName() + " took "
				+ (end - middleEnd) + " MilliSeconds");
	}

	@Test
	public void testConnection() {
		client = new SimpleClient();
		Metadata metadata = client.connect("musicdb", "127.0.0.1", "127.0.0.2", "127.0.0.3");
		System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
		for (Host host : metadata.getAllHosts()) {
			System.out.printf("Datacenter: %s; Host: %s; Rack: %s\n", host.getDatacenter(),
					host.getAddress(), host.getRack());
		}
		assertNotNull(metadata);
	}

	@Test
	public void testQuery() {
		ResultSet rs = client.executeQuery("SELECT * FROM performer LIMIT 10");
		System.out.println(rs.toString());

		List<Row> rows = rs.all();
		assertNotNull(rows);
		assertEquals(10, rows.size());

		for (Row row : rows) {
			System.out.println(String.format("%-30s\t%-20s\t%-20s", row.getString("name"),
					row.getString("style"), row.getString("country")));
		}

	}

	@Test
	public void testInsert() {
		ResultSet rs = client
				.executeQuery("INSERT INTO performer (name,country,style) VALUES ('Diablo Swing Orchestra','Sweden','Metal-Swing')");
		assertNotNull(rs);
	}

	@Test
	public void testInsertWithPreparedStatement() {
		PreparedStatement statement = client.getSession().prepare(
				"INSERT INTO musicdb.performer " + "(name,country,style) " + "VALUES (?, ?, ?);");
		BoundStatement boundStatement = new BoundStatement(statement);
		ResultSet rs = client.getSession().execute(
				boundStatement.bind("Diablo Swing Orchestra", "Sweden", "Metal-Swing"));
		assertNotNull(rs);
	}
}

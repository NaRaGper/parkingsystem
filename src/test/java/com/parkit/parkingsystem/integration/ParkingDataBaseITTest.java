package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseITTest {

	private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
	private static ParkingSpotDAO parkingSpotDAO;
	private static TicketDAO ticketDAO;
	private static DataBasePrepareService dataBasePrepareService;

	@Mock
	private static InputReaderUtil inputReaderUtil;

	@BeforeAll
	private static void setUp() throws Exception {
		parkingSpotDAO = new ParkingSpotDAO();
		parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
		ticketDAO = new TicketDAO();
		ticketDAO.dataBaseConfig = dataBaseTestConfig;
		dataBasePrepareService = new DataBasePrepareService();
	}

	@BeforeEach
	private void setUpPerTest() throws Exception {
		when(inputReaderUtil.readSelection()).thenReturn(1);
		when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
		dataBasePrepareService.clearDataBaseEntries();
	}

	@AfterAll
	private static void tearDown() {
		dataBasePrepareService.clearDataBaseEntries();
	}

	@Test
	public void testParkingACar() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/prod?serverTimezone=Europe/Amsterdam&amp", "root", "rootroot");
			Statement statement = connection.createStatement();

			// If a ticket where a vehicle with the registration number "ABCDEF" is found,
			// then isTicketCreated will return "true"
			boolean isTicketCreated = statement
					.executeQuery("SELECT vehicle_reg_number FROM ticket WHERE vehicle_reg_number='ABCDEF'").next();

			// Creating the variable isParkingSpotAvailable, where 1 means true and 0 false
			int isParkingSpotAvailable;
			ResultSet parking = statement.executeQuery("SELECT available FROM parking");

			// Getting to the first parking spot
			parking.next();
			isParkingSpotAvailable = parking.getInt(1);

			assertEquals(true, isTicketCreated);
			assertEquals(0, isParkingSpotAvailable); // It should not be available, returning 0

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Database not found.");
		}
	}

	@Test
	public void testParkingLotExit() { // Test if fare generated and the getOutTime are the same in the database and on
										// the ticket generated by the Java code
		testParkingACar();
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processExitingVehicle();

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/prod?serverTimezone=Europe/Amsterdam&amp", "root", "rootroot");
			Statement statement = connection.createStatement();

			// Searching for a vehicle regulation number given as "ABCDEF"
			ResultSet ticket = statement
					.executeQuery("SELECT price, out_time FROM ticket WHERE vehicle_reg_number='ABCDEF'");

			// getting to the first entry corresponding
			ticket.next();

			// Getting the fare from the database...
			double getFareGenerated = ticket.getDouble(1);
			// ...and the outTime
			Timestamp getOutTimeGenerated = ticket.getTimestamp(2);

			assertEquals(getFareGenerated, ticketDAO.getTicket("ABCDEF").getPrice());
			assertEquals(getOutTimeGenerated, ticketDAO.getTicket("ABCDEF").getOutTime());

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Database not found.");
		}

	}

	@Test
	public void testHasBeenHere() {
		ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
		parkingService.processIncomingVehicle();

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/prod?serverTimezone=Europe/Amsterdam&amp", "root", "rootroot");
			Statement statement = connection.createStatement();

			// Counting the number of times 'ABCDEF' has entered the parking
			ResultSet resultSet = statement
					.executeQuery("SELECT count(vehicle_reg_number) FROM ticket WHERE vehicle_reg_number='ABCDEF'");

			// Getting to the first entry
			resultSet.next();

			int numberOfTimes = resultSet.getInt(1);

			assertEquals(1, numberOfTimes); // 'ABCDEF' should have entered for the first time in the parking

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Database not found.");
		}
	}

}
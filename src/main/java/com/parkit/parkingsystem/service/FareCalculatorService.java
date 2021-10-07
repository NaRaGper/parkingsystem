package com.parkit.parkingsystem.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public static double getRateType(Ticket ticket) {
		switch (ticket.getParkingSpot().getParkingType()) {
		case CAR:
			return Fare.CAR_RATE_PER_HOUR;
		case BIKE:
			return Fare.BIKE_RATE_PER_HOUR;
		default:
			throw new IllegalArgumentException("Invalid Type");
		}
	}

	public static boolean hasBeenThere(Ticket ticket) {
		int numberOfTimes = 0;
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");

			Connection connection = DriverManager.getConnection(
					"jdbc:mysql://localhost:3306/prod?serverTimezone=Europe/Amsterdam&amp", "root", "rootroot");
			Statement statement = connection.createStatement();

			// Counting the number of times a given regNumber has entered the parking
			ResultSet resultSet = statement.executeQuery(
					"SELECT COUNT(*) FROM ticket WHERE vehicle_reg_number='" + ticket.getVehicleRegNumber() + "'");

			// Getting to the first entry
			resultSet.next();

			numberOfTimes = resultSet.getInt(1);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Database not found.");
		}
		if (numberOfTimes <= 1) {
			return false;
		} else {
			return true;
		}
	}

	public void calculateFare(Ticket ticket) {
		if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
			throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
		}

		long inTime = ticket.getInTime().getTime();
		long outTime = ticket.getOutTime().getTime();
		double ticketPrice;
		double ticketPriceRounded;
		long duration = outTime - inTime; // The duration is in milliseconds

		if (duration <= 30 * (1000 * 60)) { // If the time parked is less than 30 minutes, then we set the ticket price
			// to 0
			ticket.setPrice(0);

		} else {
			// We calculate the fare times the duration (in milliseconds) and convert the
			// total back to hourly rates
			ticketPrice = ((((duration * getRateType(ticket)) / 1000) / 60) / 60);

			if (hasBeenThere(ticket) == true)
				ticketPrice *= 0.95; // We apply the 5% discount

			// Then round it up to 2 decimal places to make it easier to read
			ticketPriceRounded = Math.round(ticketPrice * 100) / 100.00;
			ticket.setPrice(ticketPriceRounded);
		}
	}
}
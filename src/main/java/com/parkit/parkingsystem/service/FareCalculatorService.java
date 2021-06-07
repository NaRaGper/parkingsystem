package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
       
        long inTime = ticket.getInTime().getTime();
        long outTime = ticket.getOutTime().getTime();

        //TODO: Triggering a default case
        long duration = outTime - inTime; //The duration is in milliseconds 
        
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice((((duration * Fare.CAR_RATE_PER_HOUR) /1000) /60) /60); //We calculate the fare * duration (in milliseconds) and convert the total back to hourly rates
                break;
            }
            case BIKE: {
                ticket.setPrice((((duration * Fare.BIKE_RATE_PER_HOUR) /1000) /60) /60);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}
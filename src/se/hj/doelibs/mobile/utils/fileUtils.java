package se.hj.doelibs.mobile.utils;

import android.content.Context;
import se.hj.doelibs.model.Loan;
import se.hj.doelibs.model.Reservation;

import java.io.*;
import java.util.List;

/**
 * Created by Alexander on 2014-12-08.
 */
public class fileUtils {

    /**
     * Writes the Loans to file
     * @param fileName
     * @param context
     * @param loans
     */
    public static void writeLoansToFile(String fileName, Context context ,List<Loan> loans)
    {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            out = new ObjectOutputStream(fos);

            out.writeObject(loans);
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Writes reservations to file
     * @param fileName
     * @param context
     * @param reservations
     */
    public static void writeReservationToFile(String fileName, Context context ,List<Reservation> reservations)
    {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            out = new ObjectOutputStream(fos);

            out.writeObject(reservations);
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns a list of loans from the given file
     * @param fileName
     * @param context
     * @return
     */
    public static List<Loan> readLoansFromFile(String fileName,Context context)
    {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        List<Loan> loans = null;
        try {
            fis = context.openFileInput(fileName);
            in = new ObjectInputStream(fis);
            loans = (List<Loan>) in.readObject();
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return loans;
    }

    /**
     * Returns a list of reservations from the given file
     * @param fileName
     * @param context
     * @return
     */
    public static List<Reservation> readReservationFromFile(String fileName,Context context)
    {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        List<Reservation> reservations = null;

        try {
            fis = context.openFileInput(fileName);
            in = new ObjectInputStream(fis);
            reservations = (List<Reservation>) in.readObject();
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return reservations;
    }

    /**
     * Returns true if file exist
     * @param fileName
     * @param context
     * @return
     */
    public static boolean fileExist(String fileName, Context context)
    {
        Boolean bExist = false;
        File file = context.getFileStreamPath(fileName);

        if( file.exists() )
        {
            bExist = true;
        }
        return bExist;
    }
}

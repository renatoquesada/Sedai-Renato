package com.sedai.ascii.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class PostcodeASCIIWriter {

    //I need to calculate these numbers, so that I can figure out the exact range of numbers I am dealing with
    //for x and y, from the data set that is coming from the CSV file.
    private static Double minLatitudeY = null;
    private static Double maxLatitudeY = null;
    private static Double minLongitudeX = null;
    private static Double maxLongitudeX = null;

    // Assumption #1: there is the possibility to specify the range of latitudes and longitudes you want to display.
    //I am thinking here: if you want to make this algorithm more flexible, defining limits here for valid
    //x and y coordinates could be used for example to say, instead of the entire UK, let's define as valid ones,
    // ranges only for ireland, etc. Also, to avoid hardcoding magic numbers. Also, for some data validation.
    // In an improved version this could be coming from a CLI.
    private static final double MIN_LIMIT_LATITUDE_Y = 49;
    private static final double MAX_LIMIT_LATITUDE_Y = 61;
    private static final double MIN_LIMIT_LONGITUDE_X = -9;
    private static final double MAX_LIMIT_LONGITUDE_X = 2;
    private static final int GRID_WIDTH = 80;
    private static final int GRID_HEIGHT = 50;

    //Assumption 2: the path is hardcoded in code. In an improved version this could be coming from a CLI.
    private static final String filePath = "/Users/user/Documents/personal/CV/Sedai/uk-postcodes-latitude-longitude-complete-csv/ukpostcodes.csv";


    private static List<Coordinate> coordinates = new LinkedList<>();

    public static void main(String[] args){
        loadValidRangeOfCoordinates();

        System.out.println("minLatitudeY: " + minLatitudeY);
        System.out.println("maxLatitudeY: " + maxLatitudeY);
        System.out.println("minLongitudeX: " + minLongitudeX);
        System.out.println("maxLongitudeX: " + maxLongitudeX);

        int[][] gridCounts = fillGridWithCounts();
        char[][] gridChars = fillGridWithChars(gridCounts);
        printGrid(gridChars);
        System.out.println();
    }

    private static class Coordinate {
        double latitudeY;
        double longitudeX;
        Coordinate(double latitudeY, double longitudeY) {
            this.latitudeY = latitudeY;
            this.longitudeX = longitudeY;
        }
    }


    private static void loadValidRangeOfCoordinates(){
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;

            while ((line = reader.readLine()) != null) {

                //Assumption #3: the first line in the file is the header, it must be skipped.
                //For example: 'id,postcode,latitude,longitude'
                if(firstLine){
                    firstLine = false;
                    continue;//jump to next loop
                }

                //Assumption #4: the file's format is correct. Expect that every line has always this format:
                //1,AB10 1XG,57.144165160000000,-2.114847768000000
                //4 comma separated values, last 2 valid Doubles.
                //Otherwise, next lines has to be modified to handle malformed CSV files.

                String[] splitLine = line.split(",");
                Double longitudeX = Double.valueOf(splitLine[3]);
                Double latitudeY = Double.valueOf(splitLine[2]);

                //Assumption #5: the data has to be validated, there could be invalid coordinates
                //this 'if' condition is to include only latitudes and longitudes that are within the valid range for UK.
                //Also, to eliminate totally invalid ones such as '1773539,EN77 1TX,99.999999000000000,0.000000000000000'
                if((latitudeY >= MIN_LIMIT_LATITUDE_Y) && (latitudeY <= MAX_LIMIT_LATITUDE_Y) &&
                        (longitudeX >= MIN_LIMIT_LONGITUDE_X) && (longitudeX <= MAX_LIMIT_LONGITUDE_X)){
                    if(minLatitudeY == null && maxLatitudeY == null){//detect first loop and load initial values
                        minLatitudeY = latitudeY;
                        maxLatitudeY = latitudeY;
                    } else if(minLatitudeY > latitudeY){
                        minLatitudeY = latitudeY;
                    } else if (maxLatitudeY < latitudeY) {
                        maxLatitudeY = latitudeY;
                    }

                    if (minLongitudeX == null && maxLongitudeX == null) {//detect first loop and load initial values
                        minLongitudeX = longitudeX;
                        maxLongitudeX = longitudeX;
                    } else if (minLongitudeX > longitudeX) {
                        minLongitudeX = longitudeX;
                    } else if (maxLongitudeX < longitudeX) {
                        maxLongitudeX = longitudeX;
                    }

                    //adding valid coordinate to arrayList
                    coordinates.add(new Coordinate(latitudeY, longitudeX));
                } else {
                    //uncomment to show what it's discarding
                    //System.out.println("Invalid coordinate(y, x): " + latitudeY + ", " + longitudeX);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public  static int[][] fillGridWithCounts(){

        //This method is about going from a List containing all coordinates to a grid (x, y) that could easily represent
        //those coordinates as ASCII characters.
        int[][] gridResult = new int[GRID_HEIGHT][GRID_WIDTH];

        //Subtracting the starting point and ending point gives me the totals. This is necessary when generating the grid.
        int totalWidth = (int)Math.round(maxLongitudeX - minLongitudeX);
        int totalHeight = (int)Math.round(maxLatitudeY - minLatitudeY);

        for (Coordinate coordinate : coordinates) {

            //This formula converts the coordinates (Doubles) to ints that represent the x and y coordinates in the grid.
            int x = (int) Math.round((coordinate.longitudeX - minLongitudeX) / (totalWidth) * (GRID_WIDTH - 1));
            int y = (int) Math.round((maxLatitudeY - coordinate.latitudeY) / (totalHeight) * (GRID_HEIGHT - 1));

            //This is about grouping coordinates that match the same sector.
            gridResult[y][x]++;
        }
        return gridResult;
    }

    public static char[][] fillGridWithChars(int[][] gridCounts){
        char[][] gridChars = new char[GRID_HEIGHT][GRID_WIDTH];

        for (int heightCounter = 0; heightCounter < GRID_HEIGHT; heightCounter++) {
            for (int widthCounter = 0; widthCounter < GRID_WIDTH; widthCounter++) {

                //Assumption #6: any characters could be used. And there is no specification for the threshold.
                //This is getting the total for every cell, this way we can use different chars to represent density.
                //The higher the count for every cell the larger (more pixel rich) char that's being used.
                //This is totally customized. What I did was to look at the counts for each cell and figure out some
                // char equivalent that makes the final ASCII image to look nice.
                int cellTotal = gridCounts[heightCounter][widthCounter];
                if (cellTotal == 0) {
                    gridChars[heightCounter][widthCounter] = ' ';
                } else if (cellTotal < 5) {
                    gridChars[heightCounter][widthCounter] = '.';
                } else if (cellTotal < 10) {
                    gridChars[heightCounter][widthCounter] = '·';
                } else if (cellTotal < 20) {
                    gridChars[heightCounter][widthCounter] = 'o';
                } else if (cellTotal < 40) {
                    gridChars[heightCounter][widthCounter] = 'O';
                } else if (cellTotal < 80) {
                    gridChars[heightCounter][widthCounter] = '#';
                } else {
                    gridChars[heightCounter][widthCounter] = '@';
                }
            }
        }
        return gridChars;
    }

    public static void printGrid(char[][] charGrid){
        // Print the grid using the chars assigned to each cell.
        for (int i = 0; i < GRID_HEIGHT; i++) {
            for (int j = 0; j < GRID_WIDTH; j++) {
                System.out.print(charGrid[i][j]);
            }
            System.out.println();
        }
    }
}

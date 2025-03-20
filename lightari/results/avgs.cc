/**
 *   @file: avgs.cc
 * @author: Nicholas Adkins
 *   @date: Feb 18 2025
 *  @brief: 
 */

#include <iostream>
#include <fstream>

using namespace std;

int main(int argc, char const *argv[]) {
    const int a = 3;
    for (int i = a; i <= a; i++) {
        string fileName = string("tnmt_") + to_string(i) + string(".txt");
        ifstream inFile(fileName);
        string line;
        std::getline(inFile, line);
        int wins = 0, total = 0;
        for (; !inFile.eof(); std::getline(inFile, line)) {
            int winner = 0;
            sscanf(line.c_str(), "%*i %*i %*i %*i %*i %i %*i %*i", &winner);
            if (winner == 0) wins++;
            total++;
        }
        if (total == 0) {cout << "Error total == 0\n";} else
        printf("Tournament #%i Win-Rate: %i/%i = %lf\n", i, wins, total, (double(wins))/(double(total)));
    }
    return 0;
}  /// main
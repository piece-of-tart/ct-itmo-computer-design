#include <iostream>
#include <omp.h>
#include <random>
#include <fstream>
#include <cstdlib>
#include <ctime>
#include <cstdio>
#include <iosfwd>
#include <string>

using namespace std;

int main(int argc, char* argv[])
{
    if (argc != 4) {
        cout << "Illegal number of arguments in command line." << endl;
        cout << "You entered:" << '\t';
        for (int i{0}; i < argc; ++i) {
            cout << argv[i] << ' ';
        }
        return 1;
    }
    string s = argv[1];
    char* end_num;
    int num_of_threads = strtol(argv[1], &end_num, 10);
    if (argv[1] == end_num) {
        cout << "Cannot convert number of threads (argv[1]) to integer" << endl;
        return 1;
    } else if (num_of_threads < -1) {
        cout << "Number of threads (argv[1]) cannot be less than -1." << "Your argv[1]=" << num_of_threads << '.' << endl;
        return 1;
    }

    ifstream in_file{argv[2]};
    if (!in_file) {
        cout << "Cannot open file " << argv[2] << " for reading." << endl;
        return 1;
    }

    double radius;
    long long attempts;

    in_file >> radius;
    in_file >> attempts;

    if (!in_file) {
        cout << "Error while trying to read params from open file" << endl;
        return 1;
    }
    in_file.close();

    double start, end;
    long long hits = 0;
    const double radius_power = radius * radius;
    int threads_num;

    start = omp_get_wtime();

    if (num_of_threads == -1) {
        double x, y;
        random_device rd;
        mt19937_64 gen(rd());
        uniform_real_distribution<double> dis(-radius, radius);
        for (long long i{0}; i < attempts; ++i) {
            x = dis(gen);
            y = dis(gen);
            if (x * x + y * y <= radius_power) {
                hits++;
            }
        }
    } else {
        if (num_of_threads != 0) {
            omp_set_num_threads(num_of_threads);
        }
#pragma omp parallel default(none) shared(hits, attempts, radius_power, radius, threads_num)
        {
            double x, y;
            const int thread_id = omp_get_thread_num();
            long long hits_in_thread = 0;
            if (thread_id == 0) {
                threads_num = omp_get_num_threads();
            }

            mt19937_64 gen(time(nullptr) ^ (thread_id << 5));
            uniform_real_distribution<double> dis(-radius, radius);
#pragma omp for schedule(dynamic, 1000) nowait
            for (int i = 0; i < attempts; ++i) {
                x = dis(gen);
                y = dis(gen);
                if (x * x + y * y <= radius_power) {
                    hits_in_thread++;
                }
            }
#pragma omp critical
            hits += hits_in_thread;
        }
    }

    end = omp_get_wtime();

    ofstream out_file{argv[3]};
    if (!out_file) {
        cout << "Cannot open file " << argv[3] << " for writing." << endl;
        return 1;
    }

    const double square_area = radius_power * 2 * 2;
    const double square = (double) hits /(double) attempts * square_area;

    out_file << square << endl;
    out_file.close();

    printf("Time (%d thread(s)): %g ms.\n", (num_of_threads == -1) ? 1 : threads_num, end - start);

    return 0;
}
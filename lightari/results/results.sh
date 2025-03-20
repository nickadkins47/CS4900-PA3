
for i in 3
do
    cat bin/tournament_$i/tournament.csv | head -n +36 | tail -n +17 > lightari/results/tnmt_$i.txt
done
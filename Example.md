## Standard with stop words
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/ALombre.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt
```
## Error
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt
```
## 1 line per split
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/forestier_mayotte.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt -nblines 1 -nbreducers 100
```
## 1 line per split, beer ...
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/Beer.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt -nblines 1 -nbreducers 100
```
## Very small file, many reducers
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/forestier_mayotte.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt -nblines 100 -nbreducers 100
```
## Small file
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/forestier_mayotte.txt
```
## Big
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/sante_publique.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt -nblines 1000 -nbreducers 100
```
## Small
```
-w /cal/homes/rpicon/git/SANDBOX -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt -i /cal/homes/rpicon/git/SANDBOX/MiniALombre.txt  -s /cal/homes/rpicon/git/SANDBOX/stop.txt -nblines 10 -nbreducers 100
```

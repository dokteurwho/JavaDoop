## Slave

Slave.jar works in two modes, "mapping" or "reducing".

### Mapping -m from SPLITx to UMx.

Mapping operation will generate from a (short) text file an output file with the count of each word of the input file.

_If a word is found twice the word is agreegated._

__Command example__

```
java -Dfile.encoding=UTF-8 -jar Slave.jar -m SPLIT_5.txt -o UM_5
```

This command will generate two file UM_5.bin and UM_5.txt where UM_5.bin is only a binary compacted version of UM_5.txt.

* where SPLIT_5.txt can be, for example:
```
Longtemps, je me suis couché de bonne heure. Parfois, à peine ma
bougie éteinte, mes yeux se fermaient si vite que je n'avais pas le
temps de me dire: «Je m'endors.» Et, une demi-heure après, la pensée
qu'il était temps de chercher le sommeil m'éveillait; je voulais poser
le volume que je croyais avoir encore dans les mains et souffler ma
lumière; je n'avais pas cessé en dormant de faire des réflexions sur
ce que je venais de lire, mais ces réflexions avaient pris un tour un
peu particulier; il me semblait que j'étais moi-même ce dont parlait
l'ouvrage: une église, un quatuor, la rivalité de François Ier et de
Charles Quint. Cette croyance survivait pendant quelques secondes à
mon réveil; elle ne choquait pas ma raison mais pesait comme des
```
* the output will be:
```
après 1
avaient 1
avais 2
avoir 1
bonne 1
bougie 1
ce 2
ces 1
cessé 1
cette 1
charles 1
chercher 1
choquait 1
<...>
```

### Reducing -r from SMx to RMx

Mapping operation will generate from an input file containing couple of (word, count) an output file aggregating the same words together:

* (hello, 5) + (hello 7) = (hello, 12).

__Command example__

```
java -Dfile.encoding=UTF-8 -jar Slave.jar -r SM_4.txt -o RM_4
```

This command will generate RM_4.txt reducing all words into unique couple (word, count).

* where SM_4.txt can be, for example:
```
bonne 1
elle 4
<...>
sur 1
une 2
venais 1
<...>
sur 1
une 5
à 2
<...>
tendrement 1
une 1
vers 1
<...>
une 2
```
* the output will be:
```
bonne 1
elle 4
sur 2
une 11
venais 1
<...>
```

#ShavaDoop

##1. Introduction

__ShavaDoop__ is a versatile framework to powerfully count words in a file with many distributed computers in a slower manner than with only one computer.

The delivery consists in two JAR files:

1. Master.JAR

2. Slave.JAR

Master will orchestrate many Slave instances to map then reduce every word of the file as presented below.

![Alt text](https://github.com/dokteurwho/JavaDoop/blob/master/ShavaDoop.png)

## 2. Master

There is one __Master__ instance in the architecture. Master will split initial file beyond slaves and then shuffle the mapped results beyond these slaves.

```
Usage: 
Master.jar [options] 
Mandatory:
-inputfile -i <file>		input file, txt format.
-workingdir -w <directory>	working directory where all file will be generated 
-slavelist -l <file> 		slave list 
Options:
-stopwordsfile -s <file>	file containing stop words, example here: http://snowball.tartarus.org/algorithms/french/stop.txt 
-nbreducers -r  integer 	number of reducers, 5 by default
-nblines -n integer  		number of line per splitted file, 1000 by default
```

### 1. Input File
A text file that will be processed to count in a distributed configuration the number of words. It is recommended to put a large file but a very small file like this one will work too:

```
Deer Beer River
Car Car River
Deer Car Beer
```

### 2. Working Directory
The place where all intermediate and final files will be stored.
This can be use for fail-over in a future version.
The assumption is a distributed FS like HDFS.

### 3. Slave list
This file will map where are the slaves on the network. A line defines a slave. The same line can be repeated many time if you want use many time the same slave.
You can use local slaves (on the same machine than the master) or remote slaves. In this case use login@machine, (with comma) before the path of the slave. Look this example: 
```
user@c130-35,/cal/homes/user/git/SANDBOX/SLAVE1
user@c130-33,/cal/homes/user/git/SANDBOX/SLAVE1
user@c130-33,/cal/homes/user/git/SANDBOX/SLAVE1
user@c130-33,/cal/homes/user/git/SANDBOX/SLAVE1
<...>
user@c130-26,/cal/homes/user/git/SANDBOX/SLAVE3
user@c130-25,/cal/homes/user/git/SANDBOX/SLAVE3
/cal/homes/user/git/SANDBOX/SLAVE1
/cal/homes/user/git/SANDBOX/SLAVE2
/cal/homes/user/git/SANDBOX/SLAVE3
/cal/homes/user/git/SANDBOX/SLAVE3
```

* In _user@c130-33,/cal/homes/user/git/SANDBOX/SLAVE1_ MASTER expects to find a slave in __/cal/homes/user/git/SANDBOX/SLAVE1/Slave.jar__ on remote machine  __user@c130-33__.

* Note _user@c130-33,/cal/homes/user/git/SANDBOX/SLAVE1_ is used twice. This means this slave will be more requested to take job.

* _/cal/homes/user/git/SANDBOX/SLAVE3_ means the slave is on the same machine than the master.

### 4. Generated file
The output file will be:  __inputfile.txt.count.txt__. It will look like this example, where most frequent word is first.

```
était	1393
où	760
swann	702
même	690
être	599
bien	517
odette	453
là	351
dit	337
mme	312
été	302
peut	289
grand	287
fois	286
<...>
```

### 5. Stop words file
If you want to remove some words from the reducing use this file as a black-list. These word will be removed. This allow ShavaDoop to be localisation independant.

Go here http://snowball.tartarus.org/algorithms/french/stemmer.html and  http://snowball.tartarus.org/algorithms/french/stop.txt to find some example you can improve.

Comment '|' are accepted.

### 6. Number of reducers
At the last step the master will shuffle (word, count) couples to slaves. Number of reducer defines the cardinality of the shuffle. For this reason it is recommended to have the same size of magnitude than the number of slaves but it is not mandatory.
The shuffling process (ie how the words are spread) is base on word hash.

### 7.Number of lines
Defines how many line will contain the input file when it will be split. The smallest the more SPLIT files will be generated.
Can be set to 1 for fun if you want to have as many SPLIT than the orginal file contains lines.

### 8. Master implementation

#### Job and Slaves
Master manages essentially two classes: Slave and Job.

* A Job is an action to perform for example: 
```
java -Dfile.encoding=UTF-8 -jar Slave.jar -r SM_4.txt -o RM_4
```

* A Slave is a computer that can perform a job. Bascically it is machine defined in _Slave List_ file.
```
user@c130-35,/cal/homes/user/git/SANDBOX/SLAVE1
```

#### Management

The way the Master attributes a Job to a Slave is basically round robin.

1. Find a Job to perform.
2. Find a free Slave (not processing a Job.) Slaves have a score depending on how many time they achieved a job. Best free Slaves will be selected first. This can conduct to non-use of some slaves.

```java
// Going through each slave, find the most efficient one.
for(SlaveExecutor slave : slaveList) {
	// A TERMINATED slave can take a new job.
	if(slave.getJobState() == State.TERMINATED) 
	{
		// If needed, update candidate.
		float current = slave.getScore();
		if(current >= best) {
			best = current;
			bestSlave = slave;
		}
	}
}

// No slave available, take a breath.
if(bestSlave == null) {
	logger.Log("No slave available");
	Thread.sleep(100);
	return 1;
}
```
where:
```java
// Update score.
public float getScore() {
	int t = failureNb + successNb;
	if(t == 0)
		return 1;
	else
		return successNb / (failureNb + successNb);
}
```

### 9. SDOUT

### Start

When starting Master a short summary is displayed. 
```
2016-11-17 19:31:08.666,Master,------ Configuration ------ 
2016-11-17 19:31:08.666,Master,Input file: /cal/homes/user/git/SANDBOX/Beer.txt
2016-11-17 19:31:08.666,Master,Working directory: /cal/homes/user/git/SANDBOX/
2016-11-17 19:31:08.666,Master,Slave list : /cal/homes/user/git/SANDBOX/slavelist.txt
2016-11-17 19:31:08.666,Master,Number of reducer  (optional, default 5): 100
2016-11-17 19:31:08.666,Master,Number of lines per files  (optional, default 1000): 1
2016-11-17 19:31:08.666,Master,Stop words list (optional, default none): /cal/homes/user/git/SANDBOX/stop.txt
2016-11-17 19:31:08.667,Master,------ Creating slaves list ------ 
2016-11-17 19:31:08.671,Master,------ Creating stop words list ------ 
2016-11-17 19:31:08.679,Master,------ Splitting files ------ 
2016-11-17 19:31:08.687,Master,------ Starting map process ------
```

#### Finish

When process is achieved, an execution summary is disaplyed, including performances.

```
2016-11-17 18:23:49.304,Master,------ Remote reducing done------ 
2016-11-17 18:23:49.304,Master,------ Starting final merge ------ 
2016-11-17 18:23:51.640,Master,Saving results in /cal/homes/user/git/SANDBOX/sante_publique.txt.count.txt
2016-11-17 18:23:51.640,Master,TOP words:
2016-11-17 18:23:51.642,Master,#0   article	46850
2016-11-17 18:23:51.642,Master,#1   santé	20201
2016-11-17 18:23:51.642,Master,#2   agence	8796
2016-11-17 18:23:51.642,Master,#3   être	8760
<...>
2016-11-17 18:23:51.644,Master,#47   si	3446
2016-11-17 18:23:51.644,Master,#48   modalités	3410
2016-11-17 18:23:51.644,Master,#49   arrêté	3402
2016-11-17 18:23:51.656,Master,------ Final merge done ------
2016-11-17 18:23:51.656,Master,------ Performances ------
2016-11-17 18:23:51.656,Master,Split time: 811 ms
2016-11-17 18:23:51.656,Master,Map time: 39785 ms
2016-11-17 18:23:51.657,Master,Reduce time: 4811 ms
2016-11-17 18:23:51.657,Master,Merge time: 2352 ms
```

#### Logs

Log format is:
```
Time,Class,Free text.
```

* Master creates a slave:
```
2016-11-17 18:23:16.004,Master,Starting Slave.jar -m /cal/homes/user/git/SANDBOX/SPLIT_128.txt -o /cal/homes/user/git/SANDBOX/UM_128
```

* A thread is created:
```
2016-11-17 18:23:16.004,Thread_262351018,Creating slave [Slave.jar -m /cal/homes/user/git/SANDBOX/SPLIT_128.txt -o /cal/homes/user/git/SANDBOX/UM_128] on [user@c130-26,/cal/homes/user/git/SANDBOX/SLAVE2]
```

* A thread terminated successfully:
```
2016-11-17 18:23:16.048,Thread_1228679063,Process finished with status: Success and returned: [c133-11/137.194.34.75] Slave counting key from Sx file /cal/homes/user/git/SANDBOX/SPLIT_109.txt...Generating /cal/homes/user/git/SANDBOX/UM_109
... job finished. Output UMx file /cal/homes/user/git/SANDBOX/UM_109
```

* A thread failed:
```
2016-11-17 19:31:09.193,Thread_2093326742,Process finished with status: Failed and returned: Error: Unable to access jarfile /Slave.jar
```


## 3. Slave

Slave.jar works in two modes, "mapping" or "reducing". The mode is defined by the argument passed to the executable.

### 1. Mapping -m from SPLITx to UMx.

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

### 2. Reducing -r from SMx to RMx

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
* the output will be the following:
```
bonne 1
elle 4
sur 2
une 11
venais 1
<...>
```
### 3. STDOUT

Slave's output is very simple. Here are some examples:
* Mapping
```
[c133-11/137.194.34.75] Slave counting key from Sx file /cal/homes/user/git/SANDBOX/SPLIT_3.txt...Generating /cal/homes/user/git/SANDBOX/UM_3
... job finished. Output UMx file /cal/homes/user/git/SANDBOX/UM_3
```
* Reducing
```
[c130-25/137.194.35.25] Slave reducing keys from SMx file /cal/homes/user/git/SANDBOX/SM_4.txt...Generating /cal/homes/user/git/SANDBOX/RM_4
... job finished. Output RMx file /cal/homes/user/git/SANDBOX/RM_4
```


===CLUSTERING PROGRAM===

Usage: java -jar colex.jar -mode <mode(s)> -data <datafile> <additional files if required>

Possible values for -mode (multiples are possible, divided by whitespace):

	align -> retrieves patterns with frequencies
	
	rank -> weighs patterns, requires additional -patterns <output of align> if run without align
	
	patterns -> combination of align and rank
	
	nocomb -> removes combination patterns, requires additional -patterns <output of rank> if run without patterns (NOT RECOMMENDED)
	
	cluster -> clusters data based on provided patterns, requires additional -patterns <output of rank> if run without patterns
	
	clean -> removes unnecessary connections from clusters, requires additional -clusters <output of cluster> if run without cluster
	
	clcl -> combination of cluster and clean, requires additional -patterns <output of rank> if run without patterns
	
	all -> combination of align, rank, cluster and clean
	
	allcl -> combination of align, rank, nocomb, cluster and clean (NOT RECOMMENDED)
	

Example: java -jar colex.jar -mode rank cluster -data hu-data.tsv -patterns hu-frequencies

=> Ranks patterns from hu-frequencies and clusters words from hu-data.tsv using these patterns.


===GRAPH PROGRAM===

Usage: java -jar colex.jar -graph -in <infile> -out <outfile-prefix> -id|-minsize|-top <value>

-id: Print single cluster with id <value>
-minsize: Print all clusters with a minimum size of <value>
-top: Print top <value> clusters


Example: java -jar colex.jar -graph -in hu-clusters -out hu -id 2

=> Prints tree for the cluster with id 2 in hu-clusters to a file called hu-2.png.
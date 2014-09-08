ccg2xml.py 20140506-01.ccg
cp 20140506-01-testbed.xml testbed.xml
ccg-test -g 20140506-01-grammar.xml -text tb.txt
#/Users/mazzei/lavori/Projects/ATLAS/softExt/SRILM/srilm/bin/macosx/ngram-count -order 2 -unk -text tb.txt -lm n.2bo -gt1min 1 -gt2min 1 -ndiscount1 -ndiscount
#ccg-test -g 20140506-01-grammar.xml -ngramorder 2 -lm n.2bo
/Users/mazzei/lavori/Projects/ATLAS/softExt/SRILM/srilm/bin/macosx/ngram-count -order 4 -unk -text tb.txt -lm n.4bo -gt1min 1 -gt2min 1 -gt3min 1 -gt4min 1 -ndiscount1 -ndiscount2 -ndiscount3 -ndiscount4
ccg-test -g 20140506-01-grammar.xml -ngramorder 4 -lm n.4bo

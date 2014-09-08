ccg2xml.py 20140506-01.ccg
cp 20140506-01-testbed.xml testbed.xml
ccg-test -g 20140506-01-grammar.xml -textfsc tb-fsc.txt
/Users/mazzei/lavori/Projects/ATLAS/softExt/SRILM/srilm/bin/macosx/fngram-count -factor-file spec-02.flm -text tb-fsc.txt -lm -unk
ccg-test -g 20140506-01-grammar.xml -flmsc spec-02.flm

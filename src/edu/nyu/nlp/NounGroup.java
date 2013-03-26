package edu.nyu.nlp;

import java.util.HashSet;
import java.util.Set;

public class NounGroup {

    public NounGroup (int s, int e) {
        this.s  = s;
        this.e = e;
    }
    
    @Override
    public boolean equals (Object o) {
        NounGroup obj = (NounGroup) o;
        return this.s == obj.s && this.e == obj.e;
    }
    
    @Override
    public int hashCode( ) {
        int hash = 1;
        hash = hash * 17 + this.s;
        hash = hash * 31 + this.e;
        return hash;     
    }
    
    public String toString() {
        return String.format("Start at : %d, End at %d", s, e);
    }
    
    private int s;
    private int e;
    
    
    public static void main (String[] args) {
        Set<NounGroup> ngs = new HashSet<NounGroup>();
        ngs.add(new NounGroup(1, 2));
        if (ngs.contains(new NounGroup(1, 2)) ) {
            System.out.println("equals works");
        }
        
        if (!ngs.contains(new NounGroup(1, 3)) ) {
            System.out.println("equals works");
        }       
    }
    
}

package ie.ucd.dempsey.app;


import lombok.Value;

@Value
public class Pair<F, S> {
    F first;
    S second;
}

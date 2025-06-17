package java.extractInterface.allKinds;
public sealed interface Sealed permits SealedImpl, SealedChild {
    void doSomethingSealed();
}

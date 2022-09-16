public enum STStoreOperationEnum {
    STORE_IN_PROGRES("store in progress"),
    STORE_COMPLETE("store complete"),
    REMOVE_IN_PROGRES("remove in progress"),
    REMOVE_COMPLETE("remove complete");
    String description;
    STStoreOperationEnum(String description){
        this.description=description;
    }

    public String getDescription() {
        return description;
    }
}

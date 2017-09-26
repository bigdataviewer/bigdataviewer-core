package bdv.viewer;

@FunctionalInterface
public interface SaveOnCloseFunction
{
	public static enum UserSaveChoice
	{
		CANCEL, YES, NO
	}

	public UserSaveChoice invokeSaveOnClose();
}

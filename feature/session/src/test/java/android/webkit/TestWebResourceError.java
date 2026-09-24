package android.webkit;

/** WebResourceError 생성자가 package-private이라 framework 패키지에 둔 테스트용 오류입니다. */
public final class TestWebResourceError extends WebResourceError {
    private final int code;

    public TestWebResourceError(int code) {
        this.code = code;
    }

    @Override
    public int getErrorCode() {
        return code;
    }

    @Override
    public CharSequence getDescription() {
        return "failure";
    }
}

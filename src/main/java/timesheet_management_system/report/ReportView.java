package timesheet_management_system.report;

public enum ReportView {
    ANGAJATI("angajati", "Pe angajați"),
    CLIENTI("clienti", "Pe clienți"),
    SARCINI("sarcini", "Pe sarcini");

    private final String slug;
    private final String title;

    ReportView(String slug, String title) {
        this.slug = slug;
        this.title = title;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public static ReportView fromSlug(String slug) {
        for (ReportView view : values()) {
            if (view.slug.equals(slug)) {
                return view;
            }
        }
        return ANGAJATI;
    }
}

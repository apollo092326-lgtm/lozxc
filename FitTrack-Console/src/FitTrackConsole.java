import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FitTrackConsole{
    static Scanner sc = new Scanner(System.in);
    static MemberRepository repo = new MySqlMemberRepository();

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== FITTRACK ===");
            System.out.println("1. Record Attendance");
            System.out.println("2. Register New Member");
            System.out.println("3. Search Member");
            System.out.println("4. Update Membership");
            System.out.println("5. View All Members");
            System.out.println("6. Attendance Reports");
            System.out.println("7. Exit");
            int choice = readInt("Choice: ");

            switch (choice) {
                case 1: recordAttendance(); break;
                case 2: registerMember(); break;
                case 3: searchMember(); break;
                case 4: updateMembership(); break;
                case 5: viewAllMembers(); break;
                case 6: attendanceReports(); break;
                case 7:
                    System.out.println("Goodbye!");
                    sc.close();
                    return;
                default:
                    System.out.println("Invalid choice! Pick 1-7.");
            }
        }
    }

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Please enter a valid number.");
            }
        }
    }

    static String readNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("[ERROR] Cannot be empty.");
        }
    }

    static MembershipPlan choosePlan() {
        while (true) {
            System.out.println("1. Yearly  2. Monthly  3. Weekly");
            int p = readInt("Select plan (1-3): ");
            if (p == 1) return new YearlyPlan();
            if (p == 2) return new MonthlyPlan();
            if (p == 3) return new WeeklyPlan();
            System.out.println("[ERROR] Choose 1, 2, or 3.");
        }
    }

    static void recordAttendance() {
        System.out.println("\n--- Record Attendance ---");
        int id = readInt("Enter Member ID: ");
        System.out.println(repo.recordAttendance(id));
    }

    static void registerMember() {
        System.out.println("\n--- Register New Member ---");
        String name = readNonEmpty("Enter member name: ");
        MembershipPlan plan = choosePlan();
        int id = repo.addMember(name, plan);
        if (id != -1) {
            System.out.println("[OK] Registered! ID: " + id + " | Plan: " + plan.getName()
                    + " | Expires: " + plan.calculateExpiry(LocalDate.now()));
        }
    }

    static void searchMember() {
        System.out.println("\n--- Search Member ---");
        int id = readInt("Enter Member ID: ");
        Member m = repo.findById(id);
        if (m == null) {
            System.out.println("[ERROR] Member not found.");
            return;
        }
        System.out.println("ID         : " + m.getMemberId());
        System.out.println("Name       : " + m.getName());
        System.out.println("Plan       : " + m.getPlanName());
        System.out.println("Expires    : " + m.getExpiryDate());
        System.out.println("Days Logged: " + m.getDaysLogged());
    }

    static void updateMembership() {
        System.out.println("\n--- Update Membership ---");
        int id = readInt("Enter Member ID: ");
        Member m = repo.findById(id);
        if (m == null) {
            System.out.println("[ERROR] Member not found.");
            return;
        }

        System.out.println("Current Name   : " + m.getName());
        System.out.println("Current Plan   : " + m.getPlanName());
        System.out.println("Current Expiry : " + m.getExpiryDate());

        System.out.print("New name (Enter to keep): ");
        String newName = sc.nextLine().trim();
        if (!newName.isEmpty()) {
            m.setName(newName);
        }

        System.out.print("Change plan? (y/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("y")) {
            MembershipPlan newPlan = choosePlan();
            if (!newPlan.getName().equals(m.getPlanName())) {
                m.setPlanName(newPlan.getName());
                m.setExpiryDate(newPlan.calculateExpiry(LocalDate.now()));
            }
        }

        repo.updateMember(m);
        System.out.println("[OK] Updated! Name: " + m.getName()
                + " | Plan: " + m.getPlanName()
                + " | Expires: " + m.getExpiryDate());
    }

    static void viewAllMembers() {
        System.out.println("\n--- All Members ---");
        List<Member> members = repo.findAll();
        if (members.isEmpty()) {
            System.out.println("No registered members yet.");
            return;
        }
        System.out.printf("%-10s %-25s %-10s %-14s %-12s%n",
                "Member ID", "Name", "Plan", "Expiry Date", "Days Logged");
        System.out.println("-----------------------------------------------------------------------");
        for (Member m : members) {
            System.out.printf("%-10d %-25s %-10s %-14s %-12d%n",
                    m.getMemberId(), m.getName(), m.getPlanName(), m.getExpiryDate(), m.getDaysLogged());
        }
        System.out.println("Total members: " + members.size());
    }

    static void attendanceReports() {
        while (true) {
            System.out.println("\n--- Attendance Reports ---");
            System.out.println("1. Weekly Attendance Report");
            System.out.println("2. Monthly Attendance Report");
            System.out.println("3. Back to Main Menu");
            int choice = readInt("Choice: ");

            if (choice == 1) {
                LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                printReport("Weekly", start, start.plusDays(6));
            } else if (choice == 2) {
                LocalDate start = LocalDate.now().withDayOfMonth(1);
                LocalDate end = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
                printReport("Monthly", start, end);
            } else if (choice == 3) {
                return;
            } else {
                System.out.println("Invalid choice! Pick 1-3.");
            }
        }
    }

    static void printReport(String label, LocalDate start, LocalDate end) {
        System.out.println("\n=== " + label.toUpperCase() + " ATTENDANCE REPORT (" + start + " to " + end + ") ===");
        List<String> rows = repo.getAttendanceSummary(start, end);
        if (rows.isEmpty()) {
            System.out.println("No attendance records for this period.");
            return;
        }
        System.out.printf("%-10s %-25s %-10s%n", "Member ID", "Name", "Days");
        System.out.println("----------------------------------------------");
        for (String row : rows) {
            System.out.println(row);
        }
    }
}

// ===================== 1. ENCAPSULATION =====================
class Member {
    private int memberId;
    private String name;
    private String planName;
    private LocalDate expiryDate;
    private int daysLogged;

    public Member(int memberId, String name, String planName, LocalDate expiryDate, int daysLogged) {
        this.memberId = memberId;
        this.name = name;
        this.planName = planName;
        this.expiryDate = expiryDate;
        this.daysLogged = daysLogged;
    }

    public int getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getPlanName() { return planName; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getDaysLogged() { return daysLogged; }

    public void setName(String name) { this.name = name; }
    public void setPlanName(String planName) { this.planName = planName; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}

// ===================== 2. INHERITANCE & POLYMORPHISM =====================
abstract class MembershipPlan {
    private String name;

    public MembershipPlan(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract LocalDate calculateExpiry(LocalDate startDate);
}

class WeeklyPlan extends MembershipPlan {
    public WeeklyPlan() { super("Weekly"); }

    @Override
    public LocalDate calculateExpiry(LocalDate startDate) {
        return startDate.plusWeeks(1);
    }
}

class MonthlyPlan extends MembershipPlan {
    public MonthlyPlan() { super("Monthly"); }

    @Override
    public LocalDate calculateExpiry(LocalDate startDate) {
        return startDate.plusMonths(1);
    }
}

class YearlyPlan extends MembershipPlan {
    public YearlyPlan() { super("Yearly"); }

    @Override
    public LocalDate calculateExpiry(LocalDate startDate) {
        return startDate.plusYears(1);
    }
}

// ===================== 3. ABSTRACTION =====================
interface MemberRepository {
    int addMember(String name, MembershipPlan plan);
    Member findById(int id);
    List<Member> findAll();
    void updateMember(Member member);
    String recordAttendance(int memberId);
    List<String> getAttendanceSummary(LocalDate start, LocalDate end);
}

class MySqlMemberRepository implements MemberRepository {
    private final String url = "jdbc:mysql://localhost:3306/fit_db";
    private final String user = "root";
    private final String pass = "";

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, pass);
    }

    @Override
    public int addMember(String name, MembershipPlan plan) {
        LocalDate expiry = plan.calculateExpiry(LocalDate.now());
        String sql = "INSERT INTO members (name, plan, expiry_date) VALUES (?, ?, ?)";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, plan.getName());
            ps.setDate(3, Date.valueOf(expiry));
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        } catch (SQLException e) {
            System.out.println("[ERROR] " + e.getMessage());
            return -1;
        }
    }

    @Override
    public Member findById(int id) {
        String sql = "SELECT m.member_id, m.name, m.plan, m.expiry_date, "
                   + "(SELECT COUNT(*) FROM attendance a WHERE a.member_id = m.member_id) AS days "
                   + "FROM members m WHERE m.member_id = ?";

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Member(
                    rs.getInt("member_id"),
                    rs.getString("name"),
                    rs.getString("plan"),
                    rs.getDate("expiry_date").toLocalDate(),
                    rs.getInt("days")
                );
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Member> findAll() {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT m.member_id, m.name, m.plan, m.expiry_date, COUNT(a.attendance_id) AS days "
                   + "FROM members m LEFT JOIN attendance a ON m.member_id = a.member_id "
                   + "GROUP BY m.member_id, m.name, m.plan, m.expiry_date ORDER BY m.member_id";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Member(
                    rs.getInt("member_id"),
                    rs.getString("name"),
                    rs.getString("plan"),
                    rs.getDate("expiry_date").toLocalDate(),
                    rs.getInt("days")
                ));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
        return list;
    }

    @Override
    public void updateMember(Member member) {
        String sql = "UPDATE members SET name = ?, plan = ?, expiry_date = ? WHERE member_id = ?";
        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getPlanName());
            ps.setDate(3, Date.valueOf(member.getExpiryDate()));
            ps.setInt(4, member.getMemberId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    @Override
    public String recordAttendance(int memberId) {
        Member m = findById(memberId);
        if (m == null) return "[ERROR] Please Register First.";

        LocalDate today = LocalDate.now();
        String checkSql = "SELECT COUNT(*) FROM attendance WHERE member_id = ? AND attendance_date = ?";
        String insertSql = "INSERT INTO attendance (member_id, attendance_date) VALUES (?, ?)";

        try (Connection con = connect()) {
            PreparedStatement check = con.prepareStatement(checkSql);
            check.setInt(1, memberId);
            check.setDate(2, Date.valueOf(today));
            ResultSet rs = check.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                return "[ERROR] " + m.getName() + " already has attendance recorded today.";
            }

            PreparedStatement insert = con.prepareStatement(insertSql);
            insert.setInt(1, memberId);
            insert.setDate(2, Date.valueOf(today));
            insert.executeUpdate();
            return "[OK] Attendance Recorded Successfully! (" + m.getName() + ")";
        } catch (SQLException e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    @Override
    public List<String> getAttendanceSummary(LocalDate start, LocalDate end) {
        List<String> rows = new ArrayList<>();
        String sql = "SELECT m.member_id, m.name, COUNT(a.attendance_id) AS days "
                   + "FROM attendance a JOIN members m ON a.member_id = m.member_id "
                   + "WHERE a.attendance_date BETWEEN ? AND ? "
                   + "GROUP BY m.member_id, m.name ORDER BY days DESC, m.member_id";

        try (Connection con = connect(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                rows.add(String.format("%-10d %-25s %-10d",
                        rs.getInt("member_id"), rs.getString("name"), rs.getInt("days")));
            }
        } catch (SQLException e) {
            rows.add("[ERROR] " + e.getMessage());
        }
        return rows;
    }
}
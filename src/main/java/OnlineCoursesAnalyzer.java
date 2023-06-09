import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {

  List<Course> courses = new ArrayList<>();

  public OnlineCoursesAnalyzer(String datasetPath) {
    BufferedReader br = null;
    String line;
    try {
      br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
      br.readLine();
      while ((line = br.readLine()) != null) {
        String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
        Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
            Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
            Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
            Double.parseDouble(info[12]), Double.parseDouble(info[13]),
            Double.parseDouble(info[14]),
            Double.parseDouble(info[15]), Double.parseDouble(info[16]),
            Double.parseDouble(info[17]),
            Double.parseDouble(info[18]), Double.parseDouble(info[19]),
            Double.parseDouble(info[20]),
            Double.parseDouble(info[21]), Double.parseDouble(info[22]));
        courses.add(course);

      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //1
  public Map<String, Integer> getPtcpCountByInst() {
    return courses.stream().collect(Collectors.groupingBy(course -> course.institution,
        Collectors.summingInt(c -> c.participants)));
  }

  //2
  public Map<String, Integer> getPtcpCountByInstAndSubject() {
    Map<String, Integer> map = courses.stream()
        .collect(Collectors.groupingBy(course -> course.institution + "-" + course.subject,
            Collectors.summingInt(c -> c.participants)));
    map = map.entrySet().stream().sorted(Map.Entry.comparingByValue((o1, o2) -> o2 - o1))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1,
            LinkedHashMap::new));
    return map;
  }

  //3
  public Map<String, List<List<String>>> getCourseListOfInstructor() {
    Map<String, List<List<String>>> map = new HashMap<>();
    for (Course cours : courses) {
      String instructor = cours.instructors;
      List<String> list0 = new ArrayList<>();
      List<String> list1 = new ArrayList<>();
      List<List<String>> list = new ArrayList<>();
      list.add(list0);
      list.add(list1);

      if (!instructor.contains(",")) {
        // 1 teacher
        if (!map.containsKey(instructor)) {
          if (!list.get(0).contains(cours.title)) {
            list.get(0).add(cours.title);
          }
          map.put(instructor, list);
        } else {
          if (!map.get(instructor).get(0).contains(cours.title)) {
            map.get(instructor).get(0).add(cours.title);
          }
        }
      } else { // many teachers:
        String[] strings = instructor.split(",");
        for (int j = 0; j < strings.length; j++) {

          if (strings[j].charAt(0) == ' ') {
            strings[j] = strings[j].substring(1);
          }
          if (!map.containsKey(strings[j])) {
            if (!list.get(1).contains(cours.title)) {
              list.get(1).add(cours.title);
            }
            map.put(strings[j], list);
            List<String> list3 = new ArrayList<>();
            List<String> list4 = new ArrayList<>();
            list = new ArrayList<>();
            list.add(list3);
            list.add(list4); //initial list again
          } else {
            if (!map.get(strings[j]).get(1).contains(cours.title)) {
              map.get(strings[j]).get(1).add(cours.title);
            }
          }
        }
      }

    }
    Map<String, List<List<String>>> m = new HashMap<>();
    map.forEach((s, lists) -> {
      List<String> list0 = lists.get(0).stream().sorted().toList();
      List<String> list1 = lists.get(1).stream().sorted().toList();
      List<List<String>> list = new ArrayList<>();
      list.add(list0);
      list.add(list1);
      m.put(s, list);
    });
    return m;
  }

  //4
  public List<String> getCourses(int topK, String by) {
    List<String> ans = new ArrayList<>();
    switch (by) {
      case ("participants") -> {
        ans = courses.stream().sorted((o1, o2) -> o2.participants - o1.participants).distinct()
            .limit(topK).map(course -> course.title).collect(Collectors.toList());
      }
      case ("hours") -> {
        ans = courses.stream().sorted(Comparator.comparingDouble(Course::getTotalHours).reversed())
            .distinct().limit(topK).map(course -> course.title).collect(Collectors.toList());
      }
      default -> ans = null;
    }
    return ans;
  }

  public List<String> searchCourses(String courseSubject, double percentAudited,
      double totalCourseHours) {
    Pattern pattern = Pattern.compile(courseSubject, Pattern.CASE_INSENSITIVE);
    return courses.stream()
        .filter(course -> pattern.matcher(course.subject).find())
        .filter(course -> course.percentAudited >= percentAudited)
        .filter(course -> course.totalHours <= totalCourseHours).distinct()
        .map(course -> course.title).sorted().collect(Collectors.toList());
  }

  //6
  public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
    Map<String, Double> midAge = courses.stream()
        .collect(Collectors.groupingBy(course -> course.number,
            Collectors.averagingDouble(c -> c.medianAge)));
    Map<String, Double> percentMale = courses.stream()
        .collect(Collectors.groupingBy(course -> course.number,
            Collectors.averagingDouble(c -> c.percentMale)));
    Map<String, Double> percentDegree = courses.stream()
        .collect(Collectors.groupingBy(course -> course.number,
            Collectors.averagingDouble(c -> c.percentDegree)));
    Map<String, Optional<Course>> name = courses.stream()
        .collect(Collectors.groupingBy(Course::getNumber,
            Collectors.maxBy(Comparator.comparing(c -> c.launchDate))));
    Map<String, Double> res = new HashMap<>();
    for (Map.Entry<String, Double> entry : midAge.entrySet()) {
      double similarity = (age - entry.getValue()) * (age - entry.getValue())
          + (gender * 100 - percentMale.get(entry.getKey())) * (gender * 100 - percentMale.get(
          entry.getKey()))
          + (isBachelorOrHigher * 100 - percentDegree.get(entry.getKey()))
          * (isBachelorOrHigher * 100 - percentDegree.get(entry.getKey()));
      if (name.get(entry.getKey()).isPresent()) {
        res.put(name.get(entry.getKey()).get().title, similarity);
      }
    }
    return res.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(10)
        .map(Map.Entry::getKey).collect(Collectors.toList());
  }

}

class Course {

  String institution;
  String number;
  Date launchDate;
  String title;
  String instructors;
  String subject;
  int year;
  int honorCode;
  int participants;
  int audited;
  int certified;
  double percentAudited;
  double percentCertified;
  double percentCertified50;
  double percentVideo;
  double percentForum;
  double gradeHigherZero;
  double totalHours;
  double medianHoursCertification;
  double medianAge;
  double percentMale;
  double percentFemale;
  double percentDegree;

  public Course(String institution, String number, Date launchDate,
      String title, String instructors, String subject,
      int year, int honorCode, int participants,
      int audited, int certified, double percentAudited,
      double percentCertified, double percentCertified50,
      double percentVideo, double percentForum, double gradeHigherZero,
      double totalHours, double medianHoursCertification,
      double medianAge, double percentMale, double percentFemale,
      double percentDegree) {
    this.institution = institution;
    this.number = number;
    this.launchDate = launchDate;
    if (title.startsWith("\"")) {
      title = title.substring(1);
    }
    if (title.endsWith("\"")) {
      title = title.substring(0, title.length() - 1);
    }
    this.title = title;
    if (instructors.startsWith("\"")) {
      instructors = instructors.substring(1);
    }
    if (instructors.endsWith("\"")) {
      instructors = instructors.substring(0, instructors.length() - 1);
    }
    this.instructors = instructors;
    if (subject.startsWith("\"")) {
      subject = subject.substring(1);
    }
    if (subject.endsWith("\"")) {
      subject = subject.substring(0, subject.length() - 1);
    }
    this.subject = subject;
    this.year = year;
    this.honorCode = honorCode;
    this.participants = participants;
    this.audited = audited;
    this.certified = certified;
    this.percentAudited = percentAudited;
    this.percentCertified = percentCertified;
    this.percentCertified50 = percentCertified50;
    this.percentVideo = percentVideo;
    this.percentForum = percentForum;
    this.gradeHigherZero = gradeHigherZero;
    this.totalHours = totalHours;
    this.medianHoursCertification = medianHoursCertification;
    this.medianAge = medianAge;
    this.percentMale = percentMale;
    this.percentFemale = percentFemale;
    this.percentDegree = percentDegree;
  }

  @Override
  public boolean equals(Object o) {
    return this.title.equals(((Course) o).title);
  }

  @Override
  public int hashCode() {
    return 17 * title.hashCode();
  }

  public double getTotalHours() {
    return this.totalHours;
  }

  public String getNumber() {
    return this.number;
  }
}
package net.office_planner.Notifications;

import net.office_planner.Meetings.MeetingRepository;
import net.office_planner.Meetings.MeetingService;
import net.office_planner.Meetings.Meetings;
import net.office_planner.User.User;
import net.office_planner.User.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
@Configuration
@EnableScheduling
public class NotificationsController {

    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private JavaMailSender mailSender;

    private SmsSenderService smsResponse;
    private SmsRequest smsRequest;

    /** Sending email notification **/
    private void sendMail(User user, Meetings meeting) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        /** Getting Full name(s) and email(s) of the Owner(s)  **/
        String email = user.getEmail();
        String fName = user.getFirstName();
        String lName = user.getLastName();


        helper.setFrom("bianalyst77@gmail.com", "Office Meeting Planner Support");
        helper.setTo(email);

        String subject = "Meeting Notification";

        String content = "<p>Hello " + fName + " " + lName + ",</p>"
                + "<p>The " + meeting.getMeeting_name() + " for  " + meeting.getOrganization().getOrganization_name() +" is scheduled to commence today at " + meeting.getStartTime() + " .</p>"
                + "<p>The venue of the meeting will be at " + meeting.getBoardroom().getBoardroom_name() + " .</p>"
                + "<p>Kindly avail yourself in time.</p>"
                + "<p><br></p>"
                + "<p>Regards,</p>"
                + "<p>Office Meeting Planner Support.</p>";

        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

    public void notificationTimer(Meetings meeting, User user) throws ExecutionException, InterruptedException {
        LocalDateTime time = LocalDateTime.of(meeting.getMeetingDate(),
                meeting.getStartTime()).minusMinutes(15);
    }

    @Scheduled(cron = "*/30 * 10 * * *")
    void sendMails() throws MessagingException, UnsupportedEncodingException {



        /** Get today's meetings **/
        List<Meetings> meetings = meetingRepository.findTodayMeetings();

        /**Testing*/

//        LocalDateTime time = LocalDateTime.of(meeting.getMeetingDate(),
//                meeting.getStartTime()).minusMinutes(15);
        /**Testing*/
        /** Sending to the Owner */
        for (Meetings meeting : meetings){

            long ownerId = meeting.getOwner_id();
            User owner = userRepository.findByOwnerId(ownerId);

            sendMail(owner, meeting);

            /** Sending to the CoOwner(s) */
            for (User user : meeting.getUsers()){

                sendMail(user, meeting);
                System.out.println("Mail Sent at " +LocalTime.now());
            }
        }

    }
    /** End of Sending email notifications **/
    /** Sending Sms notifications **/

    void sendSMS(User user, Meetings meeting){

        String phoneNumber = user.getPhone();
//        String fName = user.getFirstName();
//        String lName = user.getLastName();
        String message = "Hi "+ user.getFirstName()+ "  "+ user.getFirstName()+ "  ." +
                "the " + meeting.getMeeting_name()+  " for  " + meeting.getOrganization().getOrganization_name() +" is scheduled to commence today at "
                + meeting.getStartTime()
                + "The venue of the meeting will be at " + meeting.getBoardroom().getBoardroom_name() +
                "Kindly avail yourself in time.";


        smsRequest.setPhoneNumber(phoneNumber);
        smsRequest.setMessage(message);

        smsResponse.sendSms(smsRequest);

    }

    // TODO: Verify twilio numbers to send sms
    @Scheduled(cron = "*/40 78 * * * *")
    void sendManySms(){

        List<Meetings> meetings = meetingRepository.findTodayMeetings();

        for (Meetings meeting : meetings){

            long ownerId = meeting.getOwner_id();
            User owner = userRepository.findByOwnerId(ownerId);

            /** Sending to the Owner(s) */
            sendSMS(owner, meeting);

            /** Sending to the CoOwner(s) */
            for (User user : meeting.getUsers()){

                sendSMS(user, meeting);
                System.out.println("Sms Sent at " +LocalTime.now());
            }
        }
        }
    /** End of Sending Sms notifications **/

}

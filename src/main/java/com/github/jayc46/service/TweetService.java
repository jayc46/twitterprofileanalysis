package com.github.jayc46.service;

import com.github.jayc46.model.UserProfile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import twitter4j.*;

import java.text.SimpleDateFormat;
import java.util.*;
@Service
public class TweetService {
    private Twitter authenticatedTwitter;
    private SimpleDateFormat sdf;

    public TweetService() {
        authenticatedTwitter = TwitterFactory.getSingleton();
        sdf = new SimpleDateFormat("HH");
    }

    private void getTweetAnalysis(UserProfile userProfile) throws TwitterException {
        List<Status> statuses = new ArrayList<Status>();
        Paging paging = new Paging(1, 100);
        statuses = authenticatedTwitter.getUserTimeline(userProfile.getScreenName(), paging);
        StringBuilder content = new StringBuilder();
        Map<Integer, Integer> tweetTiming = new HashMap<Integer, Integer>();
        List<HashtagEntity> hashtags = new ArrayList<HashtagEntity>();
        List<UserMentionEntity> userMentionEntities=new ArrayList<UserMentionEntity>();

        for (Status status : statuses) {
            //uncomment to avoid replies
            // content.append(status.getInReplyToScreenName() != null ? "" : status.getText() + " ");

            //uncomment to avoid retweets
            // content.append(status.getText().contains("RT") ? "" : status.getText() + " ");

            content.append(status.getText() + " ");
            hashtags.addAll(Arrays.asList(status.getHashtagEntities()));
            userMentionEntities.addAll(Arrays.asList(status.getUserMentionEntities()));
            Integer hour = Integer.valueOf(sdf.format(status.getCreatedAt()));
            tweetTiming.put(hour, tweetTiming.containsKey(hour) ? tweetTiming.get(hour) + 1 : 1);

        }
        for (UserMentionEntity userMentionEntity:userMentionEntities){
            String screenName=userMentionEntity.getScreenName();
            userProfile.getMentionsByCount().put(screenName,userProfile.getMentionsByCount().containsKey(screenName)?
                    userProfile.getMentionsByCount().get(screenName)+1:1);
        }
        for (HashtagEntity hashtagEntity : hashtags) {
            String hashtag = hashtagEntity.getText();
            userProfile.getHashtagBycount().put(hashtag, userProfile.getHashtagBycount().containsKey(hashtag) ?
                    userProfile.getHashtagBycount().get(hashtag) + 1 : 1);
        }
        userProfile.setTweetTiming(tweetTiming);
        Map<String, Integer> wordsCounts = new HashMap<String, Integer>();
        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>();
        for (String i : content.toString().split(" ")) {

            if (StringUtils.isAlpha(i) && StringUtils.isAsciiPrintable(i))
                wordsCounts.put(i, wordsCounts.containsKey(i) ? wordsCounts.get(i) + 1 : 1);
        }

        userProfile.setRetweetCount(wordsCounts.remove("RT"));
        userProfile.setOriginalTweetCount(100 - userProfile.getRetweetCount());

        list.addAll(wordsCounts.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        for (Map.Entry<String,Integer> wordByCount:list)
        {
            userProfile.getWordByFrequency().put(wordByCount.getKey(),wordByCount.getValue());

        }
        String tweetFrequency = calcStatusFrequency(statuses.get(0).getCreatedAt().getTime(),
                statuses.get(statuses.size() - 1).getCreatedAt().getTime());
        userProfile.setStatusFrequency(tweetFrequency.split(":")[0]);
        userProfile.setTweetPerDay(tweetFrequency.split(":")[1]);
    }

    private void profileAnalysis(UserProfile userProfile) throws TwitterException {
        User user = authenticatedTwitter.showUser(userProfile.getScreenName());
        userProfile.setName(user.getName());
        userProfile.setJoinedOn(user.getCreatedAt().toString());
        userProfile.setLocation(user.getLocation());
        Locale loc = new Locale(user.getLang());
        String language = loc.getDisplayLanguage(loc);
        userProfile.setLanguage(language);
        userProfile.setBio(user.getDescription());
        userProfile.setProfileURL(user.getURL());
        userProfile.setTimeZone(user.getTimeZone());
        userProfile.setTweetCount(user.getStatusesCount());
        userProfile.setFollowingCount(user.getFriendsCount());
        userProfile.setFollowerCount(user.getFollowersCount());
        userProfile.setRatio((float) userProfile.getFollowerCount() / userProfile.getFollowingCount());
    }

    private String calcStatusFrequency(long recent, long start) {
        long days = (recent - start) / (1000 * 60 * 60 * 24);
        float avgTweetCount = (float) 100 / days;
        String frequency = null;
        if (avgTweetCount > 5) //more than 5 tweets per day on average-Frequent
            frequency = "Frequent";
        else {
            if (avgTweetCount < 1) //less than 1 tweets per day on average-Rare
                frequency = "Rare";
            else
                frequency = "Moderate"; //between 1-5 tweets per day on average-Moderate
        }
        return frequency + ":" + avgTweetCount;
    }

    public UserProfile getAnalysis(String screenName) throws TwitterException {
        UserProfile userProfile = new UserProfile();
        userProfile.setScreenName(screenName);
        profileAnalysis(userProfile);
        getTweetAnalysis(userProfile);
        return userProfile;
    }
}

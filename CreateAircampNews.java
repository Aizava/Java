package com.monotrack.methods;

import com.google.common.base.Objects;
import com.monotrack.common.callback.Callback;
import com.monotrack.entities.AircampNews;
import com.monotrack.entities.AircampNewsConnections;
import com.monotrack.entities.User;
import com.monotrack.rpcserver.RpcQueryMethod;
import com.monotrack.rpcserver.exceptions.AccessDeniedRpcException;
import com.monotrack.rpcserver.exceptions.EmptyFieldRpcException;
import com.monotrack.rpcserver.exceptions.WrongTokenRpcException;
import com.monotrack.utils.hib.HibernateUtil;
import com.monotrack.utils.token.TokenUtils;
import com.monotrack.utils.token.WrongTokenException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class CreateAircampNews implements RpcQueryMethod<CreateAircampNews.Query> {

    public static class Query {

        public String token;
        public String title;
        public String text;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("token", token)
                    .add("title", title)
                    .add("text", text)
                    .toString();
        }
    }

    public static class Answer {

        public CustomNews news;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("news", news)
                    .toString();
        }
    }

    public static class CustomNews {

        public Long id;
        public String title;
        public String text;
        public Long creation_date;
        public Long last_edit_time;
        public CustomUser creator;
        public Boolean is_extinguished;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("id", id)
                    .add("title", title)
                    .add("text", text)
                    .add("creation_date", creation_date)
                    .add("last_edit_time", last_edit_time)
                    .add("creator", creator)
                    .add("is_extinguished", is_extinguished)
                    .toString();
        }
    }

    public static class CustomUser {

        public Long id;
        public String first_name;
        public String second_name;
        public String photo_url;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("id", id)
                    .add("first_name", first_name)
                    .add("second_name", second_name)
                    .add("photo_url", photo_url)
                    .toString();
        }
    }

    @Override
    public String getName() {
        return "news/add";
    }

    @Override
    public void execute(Query query, Callback<Object> callback) {
        if (query.token == null || query.title == null) {
            callback.onException(new EmptyFieldRpcException("mandatory parameters are missing"));
            return;
        }
        final User requester;
        try {
            requester = tokenUtils.getUserByToken(query.token);
        } catch (WrongTokenException e) {
            callback.onException(new WrongTokenRpcException());
            return;
        }
        if (!requester.isUserAdmin()) {
            callback.onException(new AccessDeniedRpcException("Only admins can use this method"));
            return;
        }
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            long postingTime = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis();
            final AircampNews final_news = new AircampNews(query.title, query.text, requester, postingTime, null);

            session.save(final_news);

            List<User> users = session.createQuery("from User").list();

            for (User user : users) {
                AircampNewsConnections connection = new AircampNewsConnections(user, final_news, 0L);
                session.save(connection);
            }

            tx.commit();
            callback.onResult(new Answer() {
                {
                    news = new CustomNews() {
                        {
                            id = final_news.getId();
                            title = final_news.getTitle();
                            text = final_news.getMessage();
                            creation_date = final_news.getCreation_time();
                            creator = new CustomUser() {
                                {
                                    id = requester.getId();
                                    first_name = requester.getFirstName();
                                    second_name = requester.getLastName();
                                    photo_url = requester.getPhotoUrl();
                                }
                            };
                        }
                    };
                }
            });
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            callback.onException(e);
        } finally {
            session.close();
        }

    }

    private TokenUtils tokenUtils;

    public CreateAircampNews(TokenUtils tokenUtils) {
        this.tokenUtils = tokenUtils;
    }

    @Override
    public Class<Query> getQueryClass() {
        return Query.class;
    }

}

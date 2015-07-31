package com.monotrack.methods;

import com.google.common.base.Objects;
import com.monotrack.common.callback.Callback;
import com.monotrack.entities.AircampNews;
import com.monotrack.entities.AircampNewsConnections;
import com.monotrack.entities.User;
import com.monotrack.rpcserver.RpcQueryMethod;
import com.monotrack.rpcserver.exceptions.AccessDeniedRpcException;
import com.monotrack.rpcserver.exceptions.BlockedUserAccessDeniedRpcException;
import com.monotrack.rpcserver.exceptions.EmptyFieldRpcException;
import com.monotrack.rpcserver.exceptions.WrongTokenRpcException;
import com.monotrack.utils.hib.HibernateUtil;
import com.monotrack.utils.token.TokenUtils;
import com.monotrack.utils.token.WrongTokenException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.ArrayList;
import java.util.List;


public class GetAircampNewsList implements RpcQueryMethod<GetAircampNewsList.Query> {

    public static class Query {

        public String token;
        public Boolean is_extinguished;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("token", token)
                    .add("is_extinguished", is_extinguished)
                    .toString();
        }
    }

    public static class Answer {

        public List<CreateAircampNews.CustomNews> news;

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("news", news.size())
                    .toString();
        }
    }

    @Override
    public String getName() {
        return "news/list";
    }


    @Override
    public void execute(Query query, Callback<Object> callback) {
        if (query.token == null) {
            callback.onException(new EmptyFieldRpcException());
            return;
        }

        User requester;
        try {
            requester = tokenUtils.getUserByToken(query.token);
        } catch (WrongTokenException e) {
            callback.onException(new WrongTokenRpcException());
            return;
        }

        if (requester.isUserBlocked()) {
            callback.onException(new BlockedUserAccessDeniedRpcException());
            return;
        }

        if (requester.isUserGuest()) {
            callback.onException(new AccessDeniedRpcException());
            return;
        }

        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            final List<CreateAircampNews.CustomNews> answer_news = new ArrayList<>();
            List<AircampNewsConnections> newsConnectionses = null;
            if (query.is_extinguished != null) {

                newsConnectionses = session.createQuery("from AircampNewsConnections where users_id =:id")
                        .setLong("id", requester.getId()).list();

                for (AircampNewsConnections con : newsConnectionses) {
                    final AircampNews system_news = (AircampNews) session.get(AircampNews.class, con.getNews().getId());
                    final Boolean is_ext = con.getIs_extinguished() == 1;
                    CreateAircampNews.CustomNews output_news = new CreateAircampNews.CustomNews() {
                        {
                            id = system_news.getId();
                            title = system_news.getTitle();
                            text = system_news.getMessage();
                            creation_date = system_news.getCreation_time();
                            last_edit_time = system_news.getUpdate_time();
                            is_extinguished = is_ext;
                            creator = new CreateAircampNews.CustomUser() {
                                {
                                    id = system_news.getCreator().getId();
                                    first_name = system_news.getCreator().getFirstName();
                                    second_name = system_news.getCreator().getLastName();
                                    photo_url = system_news.getCreator().getPhotoUrl();
                                }
                            };
                        }
                    };
                    answer_news.add(output_news);
                }

            } else {
                List<AircampNews> systems_news = session.createQuery("from AircampNews").list();

                for (final AircampNews sn : systems_news) {
                    CreateAircampNews.CustomNews output_news = new CreateAircampNews.CustomNews() {
                        {
                            id = sn.getId();
                            title = sn.getTitle();
                            text = sn.getMessage();
                            creation_date = sn.getCreation_time();
                            last_edit_time = sn.getUpdate_time();
                            creator = new CreateAircampNews.CustomUser() {
                                {
                                    id = sn.getCreator().getId();
                                    first_name = sn.getCreator().getFirstName();
                                    second_name = sn.getCreator().getLastName();
                                    photo_url = sn.getCreator().getPhotoUrl();
                                }
                            };
                        }
                    };
                    answer_news.add(output_news);
                }
            }


            tx.commit();
            callback.onResult(new Answer() {
                {
                    news = answer_news;
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

    public GetAircampNewsList(TokenUtils tokenUtils) {
        this.tokenUtils = tokenUtils;
    }

    @Override
    public Class<Query> getQueryClass() {
        return Query.class;
    }

}

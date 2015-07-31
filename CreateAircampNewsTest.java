package com.monotrack.methods;

import com.monotrack.entities.AircampNewsConnections;
import com.monotrack.entities.User;
import com.monotrack.rpcserver.BaseTestClient;
import com.monotrack.rpcserver.exceptions.AccessDeniedRpcException;
import com.monotrack.rpcserver.exceptions.EmptyFieldRpcException;
import com.monotrack.utils.token.TokenUtils;
import com.monotrack.utils.token.WrongTokenException;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class CreateAircampNewsTest extends BaseTestClient {

    CreateAircampNews method;
    CreateAircampNews.Query query;

    @Mock
    TokenUtils tokenUtils;

    User user;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        user = createUser("user").user;
        setTokenUser(admin);

        method = new CreateAircampNews(tokenUtils);
        query = new CreateAircampNews.Query();

        initDomainModel(admin, admin, null);
    }

    @Test(expected = EmptyFieldRpcException.class, timeout = TEN_SECONDS)
    public void emptyField() throws Throwable {
        method.execute(query, callback);
        callback.await();
    }

    @Test(expected = AccessDeniedRpcException.class, timeout = TEN_SECONDS)
    public void guestAccessDenied() throws Throwable {
        user = createUser("guest").user;
        setTokenUser(user);
        query.token = "123";
        query.title = "123";
        query.text = "123";
        method.execute(query, callback);
        callback.await();
    }

    @Test(timeout = TEN_SECONDS)
    public void passes() throws Throwable {
        TestUser user1 = createUser("user");
        TestUser user2 = createUser("user");
        query.token = "123";
        query.title = "title";
        query.text = "text";
        method.execute(query, callback);
        CreateAircampNews.Answer answer = getAnswer(CreateAircampNews.Answer.class, callback);
        assertNotNull(answer.news);
        assertEquals(answer.news.creator.id, admin.getId());
        assertEquals(answer.news.title, "title");
        assertEquals(answer.news.text, "text");

        @SuppressWarnings("uncheched")
        List<AircampNewsConnections> newsConnectionsList = executeAction(new Action<List<AircampNewsConnections>>() {
            @Override
            public List<AircampNewsConnections> execute(Session session) throws Exception {
                return session.createQuery("from AircampNewsConnections").list();
            }
        });
        assertEquals(newsConnectionsList.size(), 5);
        Set<Long> users = new HashSet<>();
        users.add(admin.getId());
        users.add(guest.getId());
        users.add(user.getId());
        users.add(user1.user.getId());
        users.add(user2.user.getId());

        for (AircampNewsConnections newsConnection : newsConnectionsList) {
            users.remove(newsConnection.getUser().getId());
            assertEquals(newsConnection.getIs_extinguished(), (Long) 0L);
            assertEquals(newsConnection.getNews().getId(), answer.news.id);
        }

        assertEquals(users.size(), 0);
    }


    private void setTokenUser(User user) throws WrongTokenException {
        when(tokenUtils.getUserByToken(anyString())).thenReturn(user);
        when(tokenUtils.getUserByToken(anyString(), any(Session.class))).thenReturn(user);
    }
}
